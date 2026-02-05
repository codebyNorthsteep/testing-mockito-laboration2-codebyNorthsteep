package com.example.payment;

public class FailedPaymentException extends RuntimeException {
    public FailedPaymentException(String message) {
        super(message);
    }
}
