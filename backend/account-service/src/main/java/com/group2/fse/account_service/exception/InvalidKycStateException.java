package com.group2.fse.account_service.exception;

public class InvalidKycStateException extends RuntimeException {
    public InvalidKycStateException(String message) {
        super(message);
    }
}
