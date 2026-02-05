package com.example.payment;

import com.example.NotificationException;

public interface EmailService {

    void sendPaymentConfirmation(String mail, double amount);

}
