package com.group2.fse.ledger_service.audit;

import org.springframework.stereotype.Component;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.service.AccountBalanceService;

/**
 * Fires a compensating (reversing) mutation against Oracle when the
 * PostgreSQL audit write fails after the primary balance mutation already
 * committed. This is NOT a real distributed-transaction rollback -- Oracle's
 * write already committed by the time we get here.
 *
 * Intended caller: Alyssa's DualWriteCoordinator, in the catch block around
 * LedgerAuditWriter#writeAuditRecord.
 */
@Component
public class CompensationManager {

    private final AccountBalanceService accountBalanceService;

    public CompensationManager(AccountBalanceService accountBalanceService) {
        this.accountBalanceService = accountBalanceService;
    }

    /**
     * Reverses a successful mutation by applying the opposite operation.
     * Caller passes the original operation type explicitly since
     * MutationResult doesn't carry it.
     *
     * @return the compensating MutationResult. If this itself fails, the
     *         account is left inconsistent and must be escalated (e.g. via
     *         AccountFlag/TransactionFlag) -- this does not retry indefinitely.
     */
    public MutationResult compensate(MutationResult originalResult, String originalType) {
        if (originalResult == null || !originalResult.isSuccess()) {
            throw new IllegalArgumentException("Cannot compensate a null or failed mutation result.");
        }

        String reversalRef = "COMP-" + originalResult.getReferenceNo();

        if ("DEBIT".equalsIgnoreCase(originalType)) {
            return accountBalanceService.executeCredit(
                originalResult.getAccountId(),
                originalResult.getMutatedAmount(),
                reversalRef
            );
        } else if ("CREDIT".equalsIgnoreCase(originalType)) {
            return accountBalanceService.executeDebit(
                originalResult.getAccountId(),
                originalResult.getMutatedAmount(),
                reversalRef
            );
        }

        throw new IllegalArgumentException("Unknown originalType for compensation: " + originalType);
    }
}