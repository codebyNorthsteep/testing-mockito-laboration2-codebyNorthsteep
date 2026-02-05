package com.example.payment;

import java.sql.PreparedStatement;

public interface PaymentRepository {

    void save(double amount, String status);

}
