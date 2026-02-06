package com.example.payment;

public interface PaymentRepository {

    PaymentStatusHandler save(PaymentStatusHandler status) throws DatabaseException;

    void update(PaymentStatusHandler status) throws DatabaseException;

}
