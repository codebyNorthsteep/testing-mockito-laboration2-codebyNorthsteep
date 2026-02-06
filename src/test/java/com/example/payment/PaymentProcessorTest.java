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


    @Test
    void should_handle_a_successful_payment_with_payment_confirmation() throws NotificationException {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);

        //Act
        boolean result = paymentProcessor.processPayment(amount, email);

        //Assert
        assertThat(result).isTrue();

        //Verify
        verify(paymentRepository).save(amount, "SUCCESS");
        verify(emailService).sendPaymentConfirmation(email, amount);

    }

    @Test
    void should_handle_a_unsuccessful_payment() throws NotificationException {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        when(paymentService.chargeSuccessful(amount)).thenReturn(false);

        //Act
        boolean result = paymentProcessor.processPayment(amount, email);

        //Assert
        assertThat(result).isFalse();

        //Verify
        verify(paymentRepository, never())
                .save(amount, "SUCCESS");
        verify(emailService, never())
                .sendPaymentConfirmation(email, amount);

    }

    // --- Edge cases, created with TDD ---

    @Test
    void should_make_successful_payment_even_if_payment_confirmation_fails() throws NotificationException {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        when(paymentService.chargeSuccessful(amount)).thenReturn(true);
        doThrow(new NotificationException("Warning! Your payment was successful but the payment confirmation was not sent."))
                .when(emailService).sendPaymentConfirmation(email, amount);

        //Act
        boolean result = paymentProcessor.processPayment(amount, email);

        //Assert
        assertThat(result).isTrue();

        //Verify
        verify(paymentRepository).save(amount, "SUCCESS");

    }

    //todo: tester för validering av input-data
    //todo: exception vid avbruten betalning/nekad

    @Test
    void should_throw_exception_if_database_fails() {
        //Arrange
        double amount = 150.00;
        String email = "very_cool_email@email.com";
        when(paymentService.chargeSuccessful(amount)).thenReturn(false);


        //act + Assert
        assertThatThrownBy(()->
                paymentProcessor.processPayment(amount, email))
                .isInstanceOf(FailedPaymentException.class)
                .hasMessageContaining("Your payment has been declined");


    }


}