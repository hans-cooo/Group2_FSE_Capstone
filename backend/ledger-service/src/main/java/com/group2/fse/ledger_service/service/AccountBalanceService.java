package com.group2.fse.ledger_service.service;

import com.group2.fse.ledger_service.dto.BalanceResponseDto;
import com.group2.fse.ledger_service.dto.DebitCreditRequestDto;
import com.group2.fse.ledger_service.dto.DebitCreditResponseDto;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponseDto;

import java.math.BigDecimal;

public interface AccountBalanceService {

    // Legacy / internal methods (for backwards compatibility with existing unit tests)
    MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo);
    MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo);
    MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo, Long actorId, String clientIp);
    MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo, Long actorId, String clientIp);

    // Enterprise API methods
    TransferResponseDto executeTransfer(TransferRequestDto request);
    TransferResponseDto executeTransfer(TransferRequestDto request, Long actorId, String clientIp);

    DebitCreditResponseDto mutateDebit(DebitCreditRequestDto request, Long actorId, String clientIp);
    DebitCreditResponseDto mutateCredit(DebitCreditRequestDto request, Long actorId, String clientIp);

    BalanceResponseDto getAccountBalance(Long accountId);
}