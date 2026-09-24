package com.group2.fse.ledger_service.service;

import java.math.BigDecimal;

import com.group2.fse.ledger_service.dto.MutationResult;

public interface AccountBalanceService {
    MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo);
    MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo);
}