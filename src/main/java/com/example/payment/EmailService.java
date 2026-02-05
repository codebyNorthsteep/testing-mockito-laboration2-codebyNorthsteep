package com.example.payment;

public interface EmailService {

    void sendPaymentConfirmation(String mail, double amount);

}
