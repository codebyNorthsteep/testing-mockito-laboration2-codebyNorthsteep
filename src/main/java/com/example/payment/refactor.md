Bryter ut beroenden - dessa injiceras sedan i PaymentProcessors konstruktor (Dependency injection)
PaymentProcessor ska inte behöva bry sig om HUR t.ex. email skickas, bara ATT det gör det.
Genom att injicera dessa via konstruktorn istället så frikopplas affärslogiken, 
vilket gör att jag kan byta ut t.ex. EmailService mot en Mock i enhetstesterna.

Databas
- DatabasConnection
  DatabaseConnection = PaymentRepository
  får istället en save-metod med inparametrarna amount och status för att användas i senare skede

Anropning av extern betaltjänst
- PaymentApi - Skapar PaymentService istället för att skapa en statisk PaymentApiResponse

EmailService
- Har nu ansvar för att skicka bekräftelse

FLOW:
- PaymentProcessor -> processPayment(amount, email)
  |
  v
- PaymentService -> chargeIsSuccessful(amount)
  |
  v
- TRUE
  |
  v
  -PaymentRepository -> save(amount, status)
  |
  v
- EmailService -> sendConfirmation()
