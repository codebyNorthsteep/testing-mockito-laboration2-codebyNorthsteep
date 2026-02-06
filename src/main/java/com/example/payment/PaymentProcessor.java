package com.example.payment;


import com.example.NotificationException;

/**
 * The PaymentProcessor class manages the process of handling payments,
 * saving their statuses, and sending payment confirmations via email.
 *
 * The class is designed with dependency injection, allowing the injection
 * of PaymentRepository, PaymentService, and EmailService implementations.
 *
 * Responsibilities:
 * - Validates the payment amount to ensure it is greater than zero.
 * - Processes payments through an external PaymentService.
 * - Saves the payment status to a repository via PaymentRepository.
 * - Sends an email confirmation using EmailService upon successful payments
 *   if a valid email address is provided.
 *
 * Key Error Handling:
 * - Throws an IllegalArgumentException if the payment amount is invalid.
 * - Throws a FailedPaymentException if the payment is declined.
 * - Throws a DatabaseException if saving to the repository fails.
 * - Logs an error without interrupting the flow when email sending fails.
 */
public class PaymentProcessor {

    //Fält för Injections
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;


    //Konstruktor för Injections
    public PaymentProcessor(PaymentRepository paymentRepository,
                            PaymentService paymentService,
                            EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;

    }


    public boolean processPayment(double amount, String email) {

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount can't be 0 or less");
        }


        // Anropar extern betaltjänst som returnerar true vid lyckad betalning
        //Gjorde från början en två metoder i PaymentService en charge och en isSuccess, risken i detta blir när två olika betalningar görs och den ena överskriver den andra
        boolean processedPayment = paymentService.chargeSuccessful(amount);

        // Anropar PaymentRepositorys save-metod vid lyckad charge
        if (!processedPayment) {
            throw new FailedPaymentException("Your payment has been declined");
        }

        try {
            paymentRepository.save(amount, "SUCCESS");
        } catch (DatabaseException e) {
            throw new DatabaseException("Database error, no payment was saved");
        }

        if (email != null && !email.isBlank()) {
            // Skickar e-post via EmailService vid lyckad charge samt valid email-adress
            try {
                emailService.sendPaymentConfirmation(email, amount);
            } catch (NotificationException e) {
                //LOgga bara felet så att koden inte dör om sendPaymentConfirmation inte funkar
                System.err.println("Warning! Your payment was successful but the payment confirmation was not sent.");
            }
        }


        return processedPayment;
    }

}
