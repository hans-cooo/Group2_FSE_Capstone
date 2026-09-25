package com.group2.fse.ledger_service.audit.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.group2.fse.ledger_service.audit.CompensationManager;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.service.AccountBalanceService;

@Service
public class CompensationManagerImpl implements CompensationManager {

    private static final Logger log = LoggerFactory.getLogger(CompensationManagerImpl.class);

    private final AccountBalanceService accountBalanceService;

    public CompensationManagerImpl(AccountBalanceService accountBalanceService) {
        this.accountBalanceService = accountBalanceService;
    }

    @Override
    public MutationResult compensateMutation(Long accountId, String mutationType, BigDecimal amount,
                                              String originalRefNo, String reason) {
        String compensationRef = "COMP-" + originalRefNo;
        log.warn("[CRITICAL_COMPENSATION_EVENT] Initiating compensating rollback for accountId={}, " +
                 "originalType={}, amount={}, originalRefNo={}, reason='{}'",
                accountId, mutationType, amount, originalRefNo, reason);

        MutationResult result;
        if ("DEBIT".equalsIgnoreCase(mutationType) || "TRANSFER_OUT".equalsIgnoreCase(mutationType)) {
            // Original operation took money away -> compensation restores money via CREDIT
            log.info("Executing compensating CREDIT for accountId={}, amount={}, ref={}", accountId, amount, compensationRef);
            result = accountBalanceService.executeCredit(accountId, amount, compensationRef);
        } else if ("CREDIT".equalsIgnoreCase(mutationType) || "TRANSFER_IN".equalsIgnoreCase(mutationType)) {
            // Original operation added money -> compensation takes money back via DEBIT
            log.info("Executing compensating DEBIT for accountId={}, amount={}, ref={}", accountId, amount, compensationRef);
            result = accountBalanceService.executeDebit(accountId, amount, compensationRef);
        } else {
            String errorMsg = "Unsupported mutation type for compensation: " + mutationType;
            log.error("[COMPENSATION_FAILED] {}", errorMsg);
            return MutationResult.failure(accountId, compensationRef, errorMsg);
        }

        if (result.isSuccess()) {
            log.info("[COMPENSATION_SUCCESS] Balance successfully restored for accountId={}, newBalance={}",
                    accountId, result.getNewBalance());
        } else {
            log.error("[FATAL_COMPENSATION_FAILURE] Could not execute compensating {} on accountId={}: {}",
                    mutationType, accountId, result.getErrorMessage());
        }

        return result;
    }
}
