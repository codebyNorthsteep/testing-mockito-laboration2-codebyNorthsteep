Bryter ut beroenden och injicerar dessa sedan i PaymentProcessors konstruktor (Dependency injection)
PaymentProcessor ska inte behöva bry sig om HUR t.ex. email skickas, bara ATT det gör det.
Genom att injicera dessa tjänster via konstruktorn istället för att hårdkoda dem så frikopplas affärslogiken från de tekniska implementationerna,
vilket i sin tur gör att jag enkelt kan byta ut exempelvis EmailService mot en Mock när jag skriver mina enhetstester.


Beroenden:

Databas
- DatabasConnection
  DatabaseConnection = PaymentRepository
  får istället en save-metod med inparametrarna amount och status för att användas i senare skede

Anropning av extern betaltjänst
- PaymentApi - Istället för att ha en statisk API nyckel, så skapar jag interfacet PaymentService som returnerar true/false beroende på om betalningen gick igenom

EmailService
- Har nu ansvar för att skicka bekräftelse men inskickad email och summa


FLOW:
- PaymentProcessor -> processPayment(amount, email)
  |
  v
- PaymentService -> chargeSuccessful(amount)
  |
  v
- TRUE
  |
  v
  -PaymentRepository -> save(amount, status)
  |
  v
- EmailService → sendPaymentConfirmation(String mail, double amount)

Egna implementationer via TDD i flödet:

- PaymentProcessor tar emot amount och email.

- Validering: Kontroll av amount.

- Vid FALSE i anrop av chargeSuccessful(amount): Kasta FailedPaymentException.

- Vid fel i anrop av save(amount, "SUCCESS"): Kasta DatabaseException.

- EmailService: Om e-postadress finns, anropas sendPaymentConfirmation.
Vid fel: Logga varning men avbryt inte flödet.

- Retur: Metoden returnerar true om hela flödet genomförts.