package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

/**
 * Contract for appending an immutable forensic record to the PostgreSQL
 * audit_store.ledger_mutation_audit table. Implementations MUST NOT set
 * previous_hash / current_hash -- those are computed by the
 * trg_compute_ledger_hash_chain DB trigger on insert.
 *
 * If this throws, callers (DualWriteCoordinator) are expected to invoke
 * CompensationManager to reverse the already-committed Oracle mutation.
 */
public interface LedgerAuditWriter {

    /**
     * @param transactionId Oracle TRANSACTION.transaction_id this audit row
     *                       corresponds to. NOTE: as of FSE-303, AccountBalanceServiceImpl
     *                       does not yet persist a Transaction row -- confirm with
     *                       Jared/Alyssa what to pass here before wiring this in.
     * @param accountId      Account whose balance moved.
     * @param transactionType e.g. "DEBIT" / "CREDIT".
     * @param amount         Mutated amount (must be > 0).
     * @param oldBalance     Balance before mutation.
     * @param newBalance     Balance after mutation.
     * @param actorId        Acting user id, nullable.
     * @param clientIp       Request-origin IP, nullable (defaults to 127.0.0.1 in DB).
     */
    void writeAuditRecord(Long transactionId,
                           Long accountId,
                           String transactionType,
                           BigDecimal amount,
                           BigDecimal oldBalance,
                           BigDecimal newBalance,
                           Long actorId,
                           String clientIp);
}