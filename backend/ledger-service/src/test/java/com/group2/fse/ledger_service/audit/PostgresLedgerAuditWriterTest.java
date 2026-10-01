package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.group2.fse.ledger_service.audit.impl.PostgresLedgerAuditWriter;

class PostgresLedgerAuditWriterTest {

    @Mock
    private JdbcTemplate auditJdbcTemplate;

    private PostgresLedgerAuditWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        writer = new PostgresLedgerAuditWriter(auditJdbcTemplate);
    }

    @Test
    void writeAuditRecord_success_doesNotThrow() {
        when(auditJdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(1);

        assertDoesNotThrow(() -> writer.writeAuditRecord(
            1L, 100L, "DEBIT",
            new BigDecimal("200.0000"), new BigDecimal("1000.0000"), new BigDecimal("800.0000"),
            null, "127.0.0.1"
        ));

        verify(auditJdbcTemplate, times(1))
            .update(anyString(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void writeAuditRecord_dbFailure_throwsAuditWriteException() {
        when(auditJdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new DataAccessResourceFailureException("connection refused"));

        assertThrows(AuditWriteException.class, () -> writer.writeAuditRecord(
            1L, 100L, "CREDIT",
            new BigDecimal("50.0000"), new BigDecimal("800.0000"), new BigDecimal("850.0000"),
            42L, "10.0.0.5"
        ));
    }
}