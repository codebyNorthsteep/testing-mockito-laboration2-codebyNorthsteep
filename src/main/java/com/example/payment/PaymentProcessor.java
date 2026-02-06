package com.example.payment;


import com.example.NotificationException;

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
        // Anropar extern betaltjänst som returnerar true vid lyckad betalning
        //Gjorde från början en två metoder i PaymentService en charge och en isSuccess, risken i detta blir när två olika betalningar görs och den ena överskriver den andra
        boolean processedPayment = paymentService.chargeSuccessful(amount);

        // Anropar PaymentRepositorys save-metod vid lyckad charge
        if (!processedPayment)
            throw new FailedPaymentException("Your payment has been declined");


        if (processedPayment) {
            paymentRepository.save(amount, "SUCCESS");

            // Skickar e-post via EmailService vid lyckad charge
            try {
                emailService.sendPaymentConfirmation(email, amount);
            } catch (NotificationException e) {
                System.err.println("Warning! Your payment was successful but the payment confirmation was not sent." + e);
            }
        }


        return processedPayment;
    }

}
