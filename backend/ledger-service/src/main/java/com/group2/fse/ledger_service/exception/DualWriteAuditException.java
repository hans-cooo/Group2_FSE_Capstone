package com.group2.fse.ledger_service.exception;

/**
 * Exception thrown when writing to the secondary PostgreSQL forensic audit database fails.
 * Being an unchecked RuntimeException, throwing this inside a @Transactional method
 * guarantees automatic rollback of the Oracle master ledger transaction (FSE-305).
 */
public class DualWriteAuditException extends RuntimeException {

    public DualWriteAuditException(String message) {
        super(message);
    }

    public DualWriteAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
