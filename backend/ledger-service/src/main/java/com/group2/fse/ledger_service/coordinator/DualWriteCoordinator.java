package com.group2.fse.ledger_service.coordinator;

import java.math.BigDecimal;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponse;

/**
 * Interface contract for coordinating dual-write synchronization across Oracle Master and PostgreSQL Forensic Audit Store.
 * Reference: docs/EPIC_B_REVIEW_AND_EPIC_C_PARALLEL_EXECUTION_PLAN.md Section 5 (Alyssa - FSE-304)
 */
public interface DualWriteCoordinator {

    /**
     * Executes atomic account-to-account transfer:
     * 1. Mutates source balance via Debit (Oracle)
     * 2. Appends immutable forensic audit entry (PostgreSQL)
     * 3. Mutates destination balance via Credit (Oracle)
     * 4. Appends immutable forensic audit entry (PostgreSQL)
     * If any step fails, triggers automated compensation to restore state invariants.
     */
    TransferResponse processTransfer(TransferRequestDto request);

    /**
     * Executes single debit mutation with synchronized PostgreSQL audit logging and automated rollback.
     */
    MutationResult processDebitWithAudit(Long accountId, BigDecimal amount, String refNo, Long actorId, String clientIp);

    /**
     * Executes single credit mutation with synchronized PostgreSQL audit logging and automated rollback.
     */
    MutationResult processCreditWithAudit(Long accountId, BigDecimal amount, String refNo, Long actorId, String clientIp);
}
