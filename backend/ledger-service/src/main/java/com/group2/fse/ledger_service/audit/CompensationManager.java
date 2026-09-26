package com.group2.fse.ledger_service.audit;

import org.springframework.stereotype.Component;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.service.AccountBalanceService;

/**
 * @deprecated As of FSE-304 / FSE-305, synchronous transactional rollback via
 * {@code @Transactional(rollbackFor = Exception.class)} and {@link com.group2.fse.ledger_service.service.DualWriteLedgerAuditService}
 * is the authoritative mechanism. Asynchronous or catch-block compensating transactions are obsolete
 * because Oracle state is cleanly rolled back before commit if the PostgreSQL audit write aborts.
 */
@Deprecated(since = "FSE-305", forRemoval = true)
@Component
public class CompensationManager {

    private final AccountBalanceService accountBalanceService;

    public CompensationManager(AccountBalanceService accountBalanceService) {
        this.accountBalanceService = accountBalanceService;
    }

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