package com.example.payment;


import com.example.NotificationException;


/**
 * The PaymentProcessor class handles the logic for processing payments,
 * interacting with a payment repository, a payment service, and an email service.
 * It validates payment inputs, manages payment statuses, ensures database updates,
 * and sends email confirmations.
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


        PaymentStatusHandler payment;
        try {
            payment = paymentRepository.save(new PaymentStatusHandler(amount, PaymentStatus.PENDING));
        } catch (DatabaseException e) {
            throw new DatabaseException("Database error, could not initialize payment in database", e);
        }

        // Anropar extern betaltjänst som returnerar true vid lyckad betalning
        //Gjorde från början en två metoder i PaymentService en charge och en isSuccess, risken i detta blir när två olika betalningar görs och den ena överskriver den andra
        boolean processedPayment = paymentService.chargeSuccessful(amount);
        try {
            if (processedPayment) {
                payment.setStatus(PaymentStatus.SUCCESS);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            paymentRepository.update(payment);
        } catch (DatabaseException e) {
            String message = processedPayment
                    ? "CRITICAL: Payment processed but DB update failed."
                    : "Database error, could not update payment status to FAILED.";
            throw new DatabaseException(message, e);
        }
        // Anropar PaymentRepositorys save-metod vid lyckad charge
        if (!processedPayment) {
            throw new FailedPaymentException("Your payment has been declined");
        }

        if (email != null && !email.isBlank()) {
            // Skickar e-post via EmailService vid lyckad charge samt valid email-adress
            try {
                emailService.sendPaymentConfirmation(email, amount);
            } catch (NotificationException e) {
                //LOgga bara felet så att koden inte dör om sendPaymentConfirmation inte funkar
                System.err.println("Warning! Your payment was successful but the payment confirmation was not sent." + e);
            }
        }


        return processedPayment;
    }

}
