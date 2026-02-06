package com.example.payment;

public class PaymentStatusHandler {
    private double amount;
    private PaymentStatus status; // PENDING, SUCCESS, FAILED

    public PaymentStatusHandler(double amount, PaymentStatus status) {
        this.amount = amount;
        this.status = status;
    }

    // Getters och Setters
    public void setStatus(PaymentStatus status) { this.status = status; }
    public PaymentStatus getStatus() { return status; }
}
