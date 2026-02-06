package com.example.payment;

public interface PaymentRepository{

    void save(double amount, String status) throws DatabaseException;

}
