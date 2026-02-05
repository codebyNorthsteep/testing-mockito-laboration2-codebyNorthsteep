Bryter ut beroenden - dessa injiceras sedan i PaymentProcessors konstruktor (Dependency injection)
Genom att injicera dessa via konstruktorn istället så frikopplas affärslogiken, 
vilket gör att jag kan byta ut t.ex. EmailService mot en Mock i enhetstesterna.

Databas
- DatabasConnection
  DatabaseConnection = PaymentRepository
  får istället en save-metod med inparametrarna amount och status för att användas i senare skede

Anropning av extern betaltjänst
- PaymentApi - Skapar PaymentService istället för att skapa en statisk PaymentApiResponse

EmailService
- Skickar bekräftelse

Flöde:
- PaymentProcessor -> processPayment(amount, email)
  |
  v
- PaymentService -> process(amount)
  |
  v
- PaymentService -> isSuccess()
  |
  v
- JA
  |
  v
  -PaymentRepository -> save(amount, status)
  |
  v
- EmailService -> sendConfirmation()
