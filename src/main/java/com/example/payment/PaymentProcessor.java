package com.example.payment;

public class PaymentProcessor {
    private static final String API_KEY = "sk_test_123456";

    public boolean processPayment(double amount) {
        // Anropar extern betaltjänst direkt med statisk API-nyckel
        PaymentApiResponse response = PaymentApi.charge(API_KEY, amount);

        // Skriver till databas direkt
        if (response.isSuccess()) {
            DatabaseConnection.getInstance()
                    .executeUpdate("INSERT INTO payments (amount, status) VALUES (" + amount + ", 'SUCCESS')");
        }

        // Skickar e-post direkt
        if (response.isSuccess()) {
            EmailService.sendPaymentConfirmation("user@example.com", amount);
        }

        return response.isSuccess();
    }

// I paketet payment finns en utkommenterad klass som heter PaymentProcessor.
// Kommentera fram koden och gör den testbar. Beroenden finns inte tillgängliga nu utan
// behöver användas via interface där implementationen av dessa kommer finnas tillgänglig I
// ett senare skede. Modifiera koden så att den blir testbar och skriv tester för den.
//  Identifiera och extrahera beroenden - tex. gör IO, pratar med externa system, side effects, gör koden svår att testa(mocka)
//  Tillämpa dependency injection - beroenden skickas in/serveras istället för att PaymentProcessor skapar dem själv
//  Skriv tester för den refaktorerade koden
//  Dokumentera dina refaktoreringsbeslut
}
