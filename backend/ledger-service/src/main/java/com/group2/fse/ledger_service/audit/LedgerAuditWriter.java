package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

/**
 * Interface contract for appending immutable balance mutation audit records into the forensic store.
 * Reference: docs/EPIC_B_REVIEW_AND_EPIC_C_PARALLEL_EXECUTION_PLAN.md Section 5 (Gabriel - FSE-305)
 */
public interface LedgerAuditWriter {

    /**
     * Appends a tamper-evident audit journal entry into the secondary forensic PostgreSQL datastore.
     *
     * @param transactionId ID of the transaction committed or staged in Oracle Master
     * @param accountId     Account ID undergoing balance mutation
     * @param refNo         Unique external reference number (UUID)
     * @param type          Mutation type (e.g. "DEBIT", "CREDIT", "TRANSFER_OUT", "TRANSFER_IN")
     * @param amount        Mutated amount
     * @param oldBal        Previous available balance prior to mutation
     * @param newBal        New available balance after mutation
     * @param actorId       ID of user/system executing the transaction
     * @param clientIp      Client IP address
     */
    void writeAuditRecord(Long transactionId, Long accountId, String refNo,
                          String type, BigDecimal amount, BigDecimal oldBal,
                          BigDecimal newBal, Long actorId, String clientIp);
}
