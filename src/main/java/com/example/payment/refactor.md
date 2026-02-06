Bryter ut beroenden och injicerar dessa sedan i PaymentProcessors konstruktor (Dependency injection)
PaymentProcessor ska inte behöva bry sig om HUR t.ex. email skickas, bara ATT det gör det.
Genom att injicera dessa tjänster via konstruktorn istället för att hårdkoda dem så frikopplas affärslogiken från de tekniska implementationerna,
vilket i sin tur gör att jag enkelt kan byta ut exempelvis EmailService mot en Mock när jag skriver mina enhetstester.


Beroenden:

Databas
- DatabasConnection
  DatabaseConnection = Skapar PaymentRepository
  Har ersatt den gamla databaskopplingen. 
  Den använder nu objektet PaymentStatusHandler och en Enum (PaymentStatus) för att hantera status på ett bättre sätt. 
  Den har både en save-metod för att initiera betalningen och en update-metod för att sätta slutstatus.

Anropning av extern betaltjänst
- PaymentApi - Istället för att ha en statisk API nyckel, så skapar jag interfacet PaymentService som returnerar true/false beroende på om betalningen gick igenom

EmailService
- Har nu ansvar för att skicka bekräftelse men inskickad email och summa


FLOW:
- PaymentProcessor -> processPayment(amount, email)
  |
  v
- PaymentRepository -> save(amount, PaymentStatus.PENDING)
  |
  v
- PaymentService -> chargeSuccessful(amount)
  |
  v
- TRUE/FALSE
  |
  v
- PaymentRepository -> update(amount, PaymentStatus.SUCCESS/FAILED)
  |
  v
- EmailService → sendPaymentConfirmation(String mail, double amount) (Endast om betalningen gick igenom)


Egna implementationer via TDD i flödet:



- PaymentProcessor tar emot amount och email.

- Validering: Kontroll av amount & email.

- Vid FALSE i anrop av chargeSuccessful(amount): Kasta FailedPaymentException. Status uppdateras till FAILED

- Vid fel i anrop av save(amount, "SUCCESS"): Kasta DatabaseException.

- EmailService: Om e-postadress är godkänt inskriven, anropas sendPaymentConfirmation.
Vid fel: Logga varning men avbryter inte flödet.

- Retur: Metoden returnerar true om hela flödet genomförts.