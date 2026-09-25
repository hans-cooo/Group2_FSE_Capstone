package com.group2.fse.ledger_service.coordinator.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.group2.fse.ledger_service.audit.AuditWriteException;
import com.group2.fse.ledger_service.audit.CompensationManager;
import com.group2.fse.ledger_service.audit.LedgerAuditWriter;
import com.group2.fse.ledger_service.coordinator.DualWriteCoordinator;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponse;
import com.group2.fse.ledger_service.service.AccountBalanceService;

@Service
public class DualWriteCoordinatorImpl implements DualWriteCoordinator {

    private static final Logger log = LoggerFactory.getLogger(DualWriteCoordinatorImpl.class);

    private final AccountBalanceService accountBalanceService;
    private final LedgerAuditWriter ledgerAuditWriter;
    private final CompensationManager compensationManager;

    public DualWriteCoordinatorImpl(AccountBalanceService accountBalanceService,
                                   LedgerAuditWriter ledgerAuditWriter,
                                   CompensationManager compensationManager) {
        this.accountBalanceService = accountBalanceService;
        this.ledgerAuditWriter = ledgerAuditWriter;
        this.compensationManager = compensationManager;
    }

    @Override
    public TransferResponse processTransfer(TransferRequestDto request) {
        if (request == null) {
            return TransferResponse.failure(null, null, null, null, "FAILED", "Transfer request cannot be null.");
        }

        String refNo = (request.getReferenceNo() != null && !request.getReferenceNo().isBlank())
                ? request.getReferenceNo()
                : UUID.randomUUID().toString();
        Long sourceAccountId = request.getSourceAccountId();
        Long destinationAccountId = request.getDestinationAccountId();
        BigDecimal amount = request.getAmount();
        Long actorId = request.getActorId();
        String clientIp = request.getClientIp();

        // 1. Validation
        if (sourceAccountId == null || destinationAccountId == null) {
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "FAILED",
                    "Source and destination account IDs are required.");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "FAILED",
                    "Source and destination accounts must be distinct.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "FAILED",
                    "Transfer amount must be greater than zero.");
        }

        log.info("Starting dual-write transfer: refNo={}, src={}, dest={}, amount={}",
                refNo, sourceAccountId, destinationAccountId, amount);

        // 2. Step 1: Debit Source Account (Oracle Master)
        MutationResult debitResult = accountBalanceService.executeDebit(sourceAccountId, amount, refNo);
        if (!debitResult.isSuccess()) {
            log.warn("Transfer rejected at source debit: refNo={}, reason={}", refNo, debitResult.getErrorMessage());
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "FAILED",
                    debitResult.getErrorMessage());
        }

        // 3. Step 2: Audit Source Debit (PostgreSQL Forensic Store)
        try {
            ledgerAuditWriter.writeAuditRecord(
                    sourceAccountId,
                    sourceAccountId,
                    refNo,
                    "TRANSFER_OUT",
                    amount,
                    debitResult.getPreviousBalance(),
                    debitResult.getNewBalance(),
                    actorId,
                    clientIp
            );
        } catch (AuditWriteException ex) {
            log.error("[DUAL_WRITE_AUDIT_FAILURE] Source debit audit failed for refNo={}. Triggering rollback.", refNo);
            compensationManager.compensateMutation(sourceAccountId, "DEBIT", amount, refNo,
                    "Audit failure on source debit: " + ex.getMessage());
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "ROLLED_BACK",
                    "Audit log failure: Source debit compensated and transaction rolled back.");
        }

        // 4. Step 3: Credit Destination Account (Oracle Master)
        MutationResult creditResult = accountBalanceService.executeCredit(destinationAccountId, amount, refNo);
        if (!creditResult.isSuccess()) {
            log.error("[TRANSFER_DESTINATION_FAILURE] Credit failed for destination account {}: {}. Compensating source.",
                    destinationAccountId, creditResult.getErrorMessage());
            compensationManager.compensateMutation(sourceAccountId, "DEBIT", amount, refNo,
                    "Destination credit failed: " + creditResult.getErrorMessage());
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "ROLLED_BACK",
                    "Destination credit failed: Source debit compensated and transaction rolled back.");
        }

        // 5. Step 4: Audit Destination Credit (PostgreSQL Forensic Store)
        try {
            ledgerAuditWriter.writeAuditRecord(
                    destinationAccountId,
                    destinationAccountId,
                    refNo,
                    "TRANSFER_IN",
                    amount,
                    creditResult.getPreviousBalance(),
                    creditResult.getNewBalance(),
                    actorId,
                    clientIp
            );
        } catch (AuditWriteException ex) {
            log.error("[DUAL_WRITE_AUDIT_FAILURE] Destination credit audit failed for refNo={}. Triggering full rollback.", refNo);
            // Compensate destination credit first
            compensationManager.compensateMutation(destinationAccountId, "CREDIT", amount, refNo,
                    "Audit failure on destination credit: " + ex.getMessage());
            // Compensate source debit
            compensationManager.compensateMutation(sourceAccountId, "DEBIT", amount, refNo,
                    "Audit failure on destination credit: " + ex.getMessage());
            return TransferResponse.failure(refNo, sourceAccountId, destinationAccountId, amount, "ROLLED_BACK",
                    "Audit log failure: Destination credit and source debit compensated. Transaction rolled back.");
        }

        log.info("Dual-write transfer completed successfully: refNo={}, srcNewBal={}, destNewBal={}",
                refNo, debitResult.getNewBalance(), creditResult.getNewBalance());

        return TransferResponse.success(
                refNo,
                sourceAccountId,
                destinationAccountId,
                amount,
                debitResult.getNewBalance(),
                creditResult.getNewBalance()
        );
    }

    @Override
    public MutationResult processDebitWithAudit(Long accountId, BigDecimal amount, String refNo,
                                                Long actorId, String clientIp) {
        String reference = (refNo != null && !refNo.isBlank()) ? refNo : UUID.randomUUID().toString();
        MutationResult result = accountBalanceService.executeDebit(accountId, amount, reference);
        if (!result.isSuccess()) {
            return result;
        }

        try {
            ledgerAuditWriter.writeAuditRecord(
                    accountId,
                    accountId,
                    reference,
                    "DEBIT",
                    amount,
                    result.getPreviousBalance(),
                    result.getNewBalance(),
                    actorId,
                    clientIp
            );
        } catch (AuditWriteException ex) {
            log.error("[DEBIT_AUDIT_FAILURE] Audit logging failed for debit ref={}. Triggering compensation.", reference);
            compensationManager.compensateMutation(accountId, "DEBIT", amount, reference,
                    "Audit logging failed: " + ex.getMessage());
            return MutationResult.failure(accountId, reference,
                    "Audit logging failed: balance debited then compensated and rolled back.");
        }

        return result;
    }

    @Override
    public MutationResult processCreditWithAudit(Long accountId, BigDecimal amount, String refNo,
                                                 Long actorId, String clientIp) {
        String reference = (refNo != null && !refNo.isBlank()) ? refNo : UUID.randomUUID().toString();
        MutationResult result = accountBalanceService.executeCredit(accountId, amount, reference);
        if (!result.isSuccess()) {
            return result;
        }

        try {
            ledgerAuditWriter.writeAuditRecord(
                    accountId,
                    accountId,
                    reference,
                    "CREDIT",
                    amount,
                    result.getPreviousBalance(),
                    result.getNewBalance(),
                    actorId,
                    clientIp
            );
        } catch (AuditWriteException ex) {
            log.error("[CREDIT_AUDIT_FAILURE] Audit logging failed for credit ref={}. Triggering compensation.", reference);
            compensationManager.compensateMutation(accountId, "CREDIT", amount, reference,
                    "Audit logging failed: " + ex.getMessage());
            return MutationResult.failure(accountId, reference,
                    "Audit logging failed: balance credited then compensated and rolled back.");
        }

        return result;
    }
}
