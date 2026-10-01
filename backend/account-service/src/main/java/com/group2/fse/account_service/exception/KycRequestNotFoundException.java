package com.group2.fse.account_service.exception;

public class KycRequestNotFoundException extends RuntimeException {
    public KycRequestNotFoundException(String message) {
        super(message);
    }
}
