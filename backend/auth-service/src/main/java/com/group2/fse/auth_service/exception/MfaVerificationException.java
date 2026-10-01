package com.group2.fse.auth_service.exception;

public class MfaVerificationException extends RuntimeException {
    public MfaVerificationException(String message) {
        super(message);
    }
}
