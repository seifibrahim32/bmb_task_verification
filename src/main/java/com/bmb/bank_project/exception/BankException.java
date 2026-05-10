package com.bmb.bank_project.exception;

import org.springframework.http.HttpStatus;

public class BankException extends RuntimeException {

    private final HttpStatus status;

    public BankException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
