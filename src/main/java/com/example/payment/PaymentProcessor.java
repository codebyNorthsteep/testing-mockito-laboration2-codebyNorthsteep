package com.example.payment;

public class PaymentProcessor {
//    private static final String API_KEY = "sk_test_123456";

    //Fält för Injections
    PaymentRepository paymentRepository;
    PaymentService paymentService;
    EmailService emailService;


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
        boolean processedPayment = paymentService.chargeSuccessfull(amount);

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

// I paketet payment finns en utkommenterad klass som heter PaymentProcessor.
// Kommentera fram koden och gör den testbar. Beroenden finns inte tillgängliga nu utan
// behöver användas via interface där implementationen av dessa kommer finnas tillgänglig I
// ett senare skede. Modifiera koden så att den blir testbar och skriv tester för den.
//  Identifiera och extrahera beroenden - tex. gör IO, pratar med externa system, side effects, gör koden svår att testa(mocka)
//  Tillämpa dependency injection - beroenden skickas in/serveras istället för att PaymentProcessor skapar dem själv - skickas in i dess konstruktor
//  Skriv tester för den refaktorerade koden
//  Dokumentera dina refaktoreringsbeslut
    //PaymenProcessor ska bara delegera och ta emot detta via konstruktor, interfacen ska göra
}
