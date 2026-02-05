package com.example.payment;


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
        if (processedPayment) {
            paymentRepository.save(amount, "SUCCESS");
        }

        // Skickar e-post via EmailService vid lyckad charge
        if (processedPayment) {
            //Ta bort den hårdkodade mailen - parameter
            emailService.sendPaymentConfirmation(email, amount);
        }

        return processedPayment;
    }

}
