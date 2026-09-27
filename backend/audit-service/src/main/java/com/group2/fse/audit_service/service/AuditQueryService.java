package com.group2.fse.audit_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.group2.fse.audit_service.dto.AuditStatementResponseDto;
import com.group2.fse.audit_service.dto.ChainVerificationResponseDto;
import com.group2.fse.audit_service.dto.ForensicAuditResponseDto;

public interface AuditQueryService {

    Page<AuditStatementResponseDto> getAccountStatement(
            Long accountId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    ForensicAuditResponseDto getTransactionForensics(Long transactionId);

    ChainVerificationResponseDto verifyChainIntegrity(Long accountId);
}