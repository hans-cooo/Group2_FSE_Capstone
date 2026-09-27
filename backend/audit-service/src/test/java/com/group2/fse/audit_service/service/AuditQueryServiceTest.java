package com.group2.fse.audit_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.group2.fse.audit_service.dto.ChainVerificationResponseDto;
import com.group2.fse.audit_service.entity.TransactionAudit;
import com.group2.fse.audit_service.repository.TransactionAuditRepository;
import com.group2.fse.audit_service.service.impl.AuditQueryServiceImpl;

class AuditQueryServiceTest {

    @Mock
    private TransactionAuditRepository transactionAuditRepository;

    @InjectMocks
    private AuditQueryServiceImpl auditQueryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testVerifyChainIntegrity_EmptyRecords() {
        when(transactionAuditRepository.findByAccountIdOrderByAuditIdAsc(1L))
                .thenReturn(Collections.emptyList());

        ChainVerificationResponseDto result = auditQueryService.verifyChainIntegrity(1L);

        assertTrue(result.isChainIntact());
        assertEquals(0, result.getTotalRecordsVerified());
    }

    @Test
    void testVerifyChainIntegrity_IntactChain() {
        TransactionAudit audit = TransactionAudit.builder()
                .auditId(101L)
                .accountId(1L)
                .transactionId(801L)
                .transactionType("DEBIT")
                .amount(new BigDecimal("100.0000"))
                .oldBalance(new BigDecimal("500.0000"))
                .newBalance(new BigDecimal("400.0000"))
                .previousHash("0000000000000000000000000000000000000000000000000000000000000000")
                .currentHash("hash_1")
                .eventTimestamp(LocalDateTime.now())
                .build();

        when(transactionAuditRepository.findByAccountIdOrderByAuditIdAsc(1L))
                .thenReturn(List.of(audit));

        ChainVerificationResponseDto result = auditQueryService.verifyChainIntegrity(1L);

        assertTrue(result.isChainIntact());
        assertEquals(1, result.getTotalRecordsVerified());
    }
}