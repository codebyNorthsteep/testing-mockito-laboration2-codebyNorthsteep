package com.example.payment;

public interface PaymentService {
    void charge(double amount);

    boolean isSuccess();
}
