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

    // --- Edge cases, created with TDD ---

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
//

    }
//
//
//
//    /**
//     * Tests the behavior of the PaymentProcessor when saving a successfully processed
//     * payment to the database fails.
//     * This test simulates a scenario where a payment has already been successfully charged
//     * via the PaymentService, but an exception is thrown while attempting to save the payment
//     * with its status in the PaymentRepository.
//     */
//    @Test
//    void should_throw_exception_if_saving_to_database_fails() {
//        //Arrange
//        double amount = 150.00;
//        String email = "very_cool_email@email.com";
//        when(paymentService.chargeSuccessful(amount)).thenReturn(true);
//        //Simulerar en databaskrash
//        doThrow(new DatabaseException("Connection lost"))
//                .when(paymentRepository).save(amount, "SUCCESS");
//
//        //act + Assert
//        assertThatThrownBy(() ->
//                paymentProcessor.processPayment(amount, email))
//                .isInstanceOf(DatabaseException.class)
//                .hasMessageContaining("Database error, no payment was saved");
//
//
//    }
//
//    /**
//     * Tests the behavior of the `processPayment` method in the `PaymentProcessor` class when provided with an invalid payment amount.
//     */
//    @Test
//    void should_throw_exception_if_amount_is_invalid() {
//        //Arrange
//        double amount = 0.0;
//        String email = "very_cool_email@email.com";
//
//        //act + assert
//        assertThatThrownBy(() ->
//                paymentProcessor.processPayment(amount, email))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("Amount can't be 0 or less");
//    }
//
//    /**
//     * Tests the behavior of the PaymentProcessor when processing a payment
//     * while the provided email address is blank or consists only of whitespace.
//     */
//    @Test
//    void should_not_call_emailService_if_email_is_blank() {
//        // Arrange
//        double amount = 150.00;
//        String email = ""; // Blank email
//        when(paymentService.chargeSuccessful(amount)).thenReturn(true);
//
//        // Act
//        boolean result = paymentProcessor.processPayment(amount, email);
//
//        // Assert
//        assertThat(result).isTrue();
//
//        // Verify
//        verify(paymentRepository).save(amount, "SUCCESS");
//        verifyNoInteractions(emailService);
//    }


}