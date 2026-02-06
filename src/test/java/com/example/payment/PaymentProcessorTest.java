package com.example.payment;

import com.example.NotificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentProcessor paymentProcessor;


    /**
     * Tests the scenario where a successful payment is processed, with correct status updates, and a payment confirmation
     * email is sent.
     */
    @Test
    void should_handle_a_successful_payment_with_payment_confirmation() throws NotificationException {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        PaymentStatusHandler payment = new PaymentStatusHandler(amount,PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);

        //Act
        boolean result = paymentProcessor.processPayment(amount, email);

        //Assert
        assertThat(result).isTrue();

        //Verify
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING));
        verify(paymentRepository).update(argThat(p -> p.getStatus() == PaymentStatus.SUCCESS));
        verify(emailService).sendPaymentConfirmation(email, amount);

    }

    /**
     * Testar att status uppdateras till FAILED om betalningen nekas.
     */
    @Test
    void should_update_to_failed_if_payment_is_declined() throws DatabaseException {
        // Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        PaymentStatusHandler payment = new PaymentStatusHandler(amount, PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> paymentProcessor.processPayment(amount, email))
                .isInstanceOf(FailedPaymentException.class);

        // Verify
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.PENDING));
        verify(paymentRepository).update(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
        verifyNoInteractions(emailService);
    }

    /**
     * Tests the behavior of the `processPayment` method in the `PaymentProcessor` class when a payment
     * is successfully charged but updating the payment status in the database fails.
     */
    @Test
    void should_throw_database_exception_if_update_fails_after_successful_charge() throws DatabaseException {
        // Arrange
        double amount = 3000.00;
        String email = "super_awesome_email@cool.com";
        PaymentStatusHandler payment = new PaymentStatusHandler(amount, PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);

        // Simuleras databaskrashen efter att betalningen genomförts
        doThrow(new DatabaseException("CRITICAL: Payment processed but DB update failed for amount: " + amount))
                .when(paymentRepository).update(any(PaymentStatusHandler.class));

        // Act + Assert
        assertThatThrownBy(() -> paymentProcessor.processPayment(amount, email))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("CRITICAL: Payment processed but DB update failed for amount: " + amount);

    }

    /**
     * Tests the behavior of the PaymentProcessor when the payment is declined by the PaymentService and that the exception is thrown.
     */

    @Test
    void should_throw_exception_if_payment_gets_declined() {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        PaymentStatusHandler payment = new PaymentStatusHandler(amount, PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(false);


        //act + Assert
        assertThatThrownBy(() ->
                paymentProcessor.processPayment(amount, email))
                .isInstanceOf(FailedPaymentException.class)
                .hasMessageContaining("Your payment has been declined");


    }


    /**
     * Tests the scenario where a payment is processed successfully, even if sending the payment
     * confirmation email fails. This test ensures that the system does not treat the failure
     * of email confirmation as critical and still considers the payment process to be
     * completed successfully.
     */
    @Test
    void should_make_successful_payment_even_if_payment_confirmation_fails() throws NotificationException {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        PaymentStatusHandler payment = new PaymentStatusHandler(amount, PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);
        doThrow(new NotificationException("Warning! Your payment was successful but the payment confirmation was not sent."))
                .when(emailService).sendPaymentConfirmation(email, amount);

        //Act
        boolean result = paymentProcessor.processPayment(amount, email);

        //Assert
        assertThat(result).isTrue();

        //Verify
        verify(paymentRepository).update(argThat(p -> p.getStatus() == PaymentStatus.SUCCESS));


    }



    /**
     * Tests the scenario where an exception is thrown during the initial save of a payment in the database.
     */
    @Test
    void should_throw_exception_if_initial_save_fails() throws DatabaseException {
        // Arrange
        double amount = 100.0;
        when(paymentRepository.save(any(PaymentStatusHandler.class)))
                .thenThrow(new DatabaseException("DB Crash"));

        // Act + Assert
        assertThatThrownBy(() -> paymentProcessor.processPayment(amount, "test@test.com"))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("Database error, could not initialize payment in database");

        // verify
        verifyNoInteractions(paymentService);
    }

    /**
     * Tests the behavior of the `processPayment` method in the `PaymentProcessor` class when provided with an invalid payment amount.
     */
    @Test
    void should_throw_exception_if_amount_is_invalid() {
        //Arrange
        double amount = 0.0;
        String email = "very_cool_email@email.com";

        //act + assert
        assertThatThrownBy(() ->
                paymentProcessor.processPayment(amount, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount can't be 0 or less");
    }

    /**
     * Tests the behavior of the PaymentProcessor when processing a payment
     * while the provided email address is blank or consists only of whitespace.
     */
    @Test
    void should_not_call_emailService_if_email_is_blank() {
        // Arrange
        double amount = 150.00;
        String email = ""; // Blank email
        PaymentStatusHandler payment = new PaymentStatusHandler(amount, PaymentStatus.PENDING);

        when(paymentRepository.save(any(PaymentStatusHandler.class))).thenReturn(payment);
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);

        // Act
        boolean result = paymentProcessor.processPayment(amount, email);

        // Assert
        assertThat(result).isTrue();

        // Verify
        verifyNoInteractions(emailService);
    }


}