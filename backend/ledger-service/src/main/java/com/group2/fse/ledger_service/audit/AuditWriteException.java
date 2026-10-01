package com.group2.fse.ledger_service.audit;

/** Thrown when the Postgres audit insert fails, so callers know to compensate. */
public class AuditWriteException extends RuntimeException {
    public AuditWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}