package com.example.payment;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}
