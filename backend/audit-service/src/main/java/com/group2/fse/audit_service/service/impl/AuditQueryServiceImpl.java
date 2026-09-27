package com.group2.fse.audit_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group2.fse.audit_service.dto.AuditStatementResponseDto;
import com.group2.fse.audit_service.dto.ChainVerificationResponseDto;
import com.group2.fse.audit_service.dto.ForensicAuditResponseDto;
import com.group2.fse.audit_service.entity.TransactionAudit;
import com.group2.fse.audit_service.repository.TransactionAuditRepository;
import com.group2.fse.audit_service.service.AuditQueryService;

@Service
public class AuditQueryServiceImpl implements AuditQueryService {

    private final TransactionAuditRepository transactionAuditRepository;

    public AuditQueryServiceImpl(TransactionAuditRepository transactionAuditRepository) {
        this.transactionAuditRepository = transactionAuditRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditStatementResponseDto> getAccountStatement(
            Long accountId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        Page<TransactionAudit> auditPage = (startDate != null && endDate != null)
                ? transactionAuditRepository.findByAccountIdAndEventTimestampBetween(accountId, startDate, endDate, pageable)
                : transactionAuditRepository.findByAccountId(accountId, pageable);

        return auditPage.map(audit -> AuditStatementResponseDto.builder()
                .auditId(audit.getAuditId())
                .transactionId(audit.getTransactionId())
                .accountId(audit.getAccountId())
                .transactionType(audit.getTransactionType())
                .amount(audit.getAmount())
                .oldBalance(audit.getOldBalance())
                .newBalance(audit.getNewBalance())
                .currentHash(audit.getCurrentHash())
                .eventTimestamp(audit.getEventTimestamp())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ForensicAuditResponseDto getTransactionForensics(Long transactionId) {
        TransactionAudit audit = transactionAuditRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Audit record not found for transaction ID: " + transactionId));

        return ForensicAuditResponseDto.builder()
                .auditId(audit.getAuditId())
                .transactionId(audit.getTransactionId())
                .accountId(audit.getAccountId())
                .transactionType(audit.getTransactionType())
                .amount(audit.getAmount())
                .oldBalance(audit.getOldBalance())
                .newBalance(audit.getNewBalance())
                .previousHash(audit.getPreviousHash())
                .currentHash(audit.getCurrentHash())
                .actorId(audit.getActorId())
                .clientIp(audit.getClientIp())
                .eventTimestamp(audit.getEventTimestamp())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChainVerificationResponseDto verifyChainIntegrity(Long accountId) {
        List<TransactionAudit> records = transactionAuditRepository.findByAccountIdOrderByAuditIdAsc(accountId);

        if (records.isEmpty()) {
            return ChainVerificationResponseDto.builder()
                    .accountId(accountId)
                    .totalRecordsVerified(0)
                    .isChainIntact(true)
                    .verifiedAt(LocalDateTime.now())
                    .message("No audit records found for account. Chain is trivially intact.")
                    .build();
        }

        String expectedPreviousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        for (int i = 0; i < records.size(); i++) {
            TransactionAudit record = records.get(i);

            if (i > 0) {
                expectedPreviousHash = records.get(i - 1).getCurrentHash();
            }

            if (record.getPreviousHash() != null && !expectedPreviousHash.equals(record.getPreviousHash())) {
                return ChainVerificationResponseDto.builder()
                        .accountId(accountId)
                        .totalRecordsVerified(records.size())
                        .isChainIntact(false)
                        .latestHash(record.getCurrentHash())
                        .verifiedAt(LocalDateTime.now())
                        .message("Chain linkage mismatch at audit ID " + record.getAuditId())
                        .build();
            }
        }

        String latestHash = records.get(records.size() - 1).getCurrentHash();
        return ChainVerificationResponseDto.builder()
                .accountId(accountId)
                .totalRecordsVerified(records.size())
                .isChainIntact(true)
                .latestHash(latestHash)
                .verifiedAt(LocalDateTime.now())
                .message("Audit chain integrity verified successfully.")
                .build();
    }
}