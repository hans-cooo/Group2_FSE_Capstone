package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.group2.fse.ledger_service.audit.impl.PostgresLedgerAuditWriter;

class PostgresLedgerAuditWriterTest {

    @Mock
    private JdbcTemplate auditJdbcTemplate;

    private PostgresLedgerAuditWriter auditWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditWriter = new PostgresLedgerAuditWriter(auditJdbcTemplate);
    }

    @Test
    void testWriteAuditRecord_Success() {
        when(auditJdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        assertDoesNotThrow(() ->
            auditWriter.writeAuditRecord(
                    100L,
                    1L,
                    "REF-100",
                    "DEBIT",
                    new BigDecimal("150.0000"),
                    new BigDecimal("1000.0000"),
                    new BigDecimal("850.0000"),
                    99L,
                    "192.168.1.10"
            )
        );

        verify(auditJdbcTemplate).update(
                anyString(),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("DEBIT"),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("150.0000")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("1000.0000")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("850.0000")),
                org.mockito.ArgumentMatchers.eq("PENDING_SHA256_HASH"),
                org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.eq("192.168.1.10")
        );
    }

    @Test
    void testWriteAuditRecord_ThrowsAuditWriteException_OnJdbcFailure() {
        when(auditJdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new QueryTimeoutException("Postgres connection timeout"));

        assertThrows(AuditWriteException.class, () ->
            auditWriter.writeAuditRecord(
                    101L,
                    2L,
                    "REF-101",
                    "CREDIT",
                    new BigDecimal("50.0000"),
                    new BigDecimal("200.0000"),
                    new BigDecimal("250.0000"),
                    99L,
                    "127.0.0.1"
            )
        );
    }
}
