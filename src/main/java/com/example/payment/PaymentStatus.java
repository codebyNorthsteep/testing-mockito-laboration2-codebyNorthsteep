package com.example.payment;

/**
 * Represents the status of a payment in the payment processing system.
 * This enum is primarily used to track and manage the lifecycle of payments
 * across various components of the payment processing flow.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
