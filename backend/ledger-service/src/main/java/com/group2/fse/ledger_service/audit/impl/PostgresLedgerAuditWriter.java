package com.group2.fse.ledger_service.audit.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.group2.fse.ledger_service.audit.AuditWriteException;
import com.group2.fse.ledger_service.audit.LedgerAuditWriter;

@Component
public class PostgresLedgerAuditWriter implements LedgerAuditWriter {

    private static final Logger log = LoggerFactory.getLogger(PostgresLedgerAuditWriter.class);

    private static final String INSERT_AUDIT_SQL = """
        INSERT INTO audit_store.ledger_mutation_audit
        (transaction_id, account_id, transaction_type, amount, old_balance, new_balance, current_hash, actor_id, client_ip)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private final JdbcTemplate auditJdbcTemplate;

    public PostgresLedgerAuditWriter(@Qualifier("auditJdbcTemplate") JdbcTemplate auditJdbcTemplate) {
        this.auditJdbcTemplate = auditJdbcTemplate;
    }

    @Override
    public void writeAuditRecord(Long transactionId, Long accountId, String refNo,
                                  String type, BigDecimal amount, BigDecimal oldBal,
                                  BigDecimal newBal, Long actorId, String clientIp) {
        log.info("Persisting immutable forensic audit record: txnId={}, accountId={}, ref={}, type={}, amount={}",
                transactionId, accountId, refNo, type, amount);

        String placeholderHash = "PENDING_SHA256_HASH";
        String ipAddress = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        try {
            auditJdbcTemplate.update(
                    INSERT_AUDIT_SQL,
                    transactionId,
                    accountId,
                    type,
                    amount,
                    oldBal,
                    newBal,
                    placeholderHash,
                    actorId,
                    ipAddress
            );
            log.debug("Successfully appended immutable audit record for txnId={}", transactionId);
        } catch (DataAccessException ex) {
            log.error("[CRITICAL_AUDIT_FAILURE] Failed to write forensic audit record to PostgreSQL for txnId={}, refNo={}: {}",
                    transactionId, refNo, ex.getMessage(), ex);
            throw new AuditWriteException(
                    "Forensic audit append failed in PostgreSQL audit store: " + ex.getMessage(),
                    transactionId,
                    accountId,
                    refNo,
                    ex
            );
        }
    }
}
