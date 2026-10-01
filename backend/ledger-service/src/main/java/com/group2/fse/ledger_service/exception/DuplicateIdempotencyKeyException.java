package com.group2.fse.ledger_service.exception;

/**
 * Thrown when an idempotency key conflict occurs or concurrent transaction is in-flight.
 * Error Code: DUPLICATE_IDEMPOTENCY_KEY / CONCURRENT_TRANSACTION_IN_FLIGHT (HTTP 409).
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}
