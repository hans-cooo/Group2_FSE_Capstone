package com.group2.fse.account_service.exception;

public class DuplicateClosureRequestException extends RuntimeException {
    public DuplicateClosureRequestException(String message) {
        super(message);
    }
}
