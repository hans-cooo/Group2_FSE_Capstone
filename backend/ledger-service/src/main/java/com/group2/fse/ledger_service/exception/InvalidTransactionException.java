package com.group2.fse.ledger_service.exception;

/**
 * Thrown when transaction parameters or business constraints fail validation.
 * Error Code: INVALID_TRANSACTION (HTTP 400).
 */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
