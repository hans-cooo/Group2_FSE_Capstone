package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

import com.group2.fse.ledger_service.dto.MutationResult;

/**
 * Rollback & Compensation Coordinator to preserve balance integrity across dual-write datastores.
 * Reference: docs/EPIC_B_REVIEW_AND_EPIC_C_PARALLEL_EXECUTION_PLAN.md Section 5 (Gabriel - FSE-305)
 */
public interface CompensationManager {

    /**
     * Executes compensating transaction on the primary Oracle balance store
     * when downstream dual-write steps (e.g. audit store write or transfer destination credit) fail.
     *
     * @param accountId       Account ID requiring compensation
     * @param mutationType    Original mutation type executed ("DEBIT" or "CREDIT")
     * @param amount          Amount originally mutated
     * @param originalRefNo   Original idempotency/transaction reference number
     * @param reason          Reason compensation was triggered
     * @return Result of the compensating balance mutation
     */
    MutationResult compensateMutation(Long accountId, String mutationType, BigDecimal amount,
                                      String originalRefNo, String reason);
}
