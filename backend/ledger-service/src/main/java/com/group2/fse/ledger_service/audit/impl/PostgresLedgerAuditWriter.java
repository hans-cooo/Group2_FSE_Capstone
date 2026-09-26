package com.group2.fse.ledger_service.audit.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.group2.fse.ledger_service.audit.AuditWriteException;
import com.group2.fse.ledger_service.audit.LedgerAuditWriter;

/**
 * @deprecated As of FSE-304/FSE-305, {@link com.group2.fse.ledger_service.service.DualWriteLedgerAuditService}
 * is the active, unified dual-write audit component providing hash-chaining and transaction logging.
 */
@Deprecated(since = "FSE-305", forRemoval = true)
@Component
public class PostgresLedgerAuditWriter implements LedgerAuditWriter {

    private static final String INSERT_SQL =
        "INSERT INTO audit_store.ledger_mutation_audit " +
        "(transaction_id, account_id, transaction_type, amount, old_balance, new_balance, actor_id, client_ip) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate auditJdbcTemplate;

    public PostgresLedgerAuditWriter(@Qualifier("auditJdbcTemplate") JdbcTemplate auditJdbcTemplate) {
        this.auditJdbcTemplate = auditJdbcTemplate;
    }

    @Override
    public void writeAuditRecord(Long transactionId,
                                  Long accountId,
                                  String transactionType,
                                  BigDecimal amount,
                                  BigDecimal oldBalance,
                                  BigDecimal newBalance,
                                  Long actorId,
                                  String clientIp) {
        try {
            auditJdbcTemplate.update(
                INSERT_SQL,
                transactionId, accountId, transactionType,
                amount, oldBalance, newBalance,
                actorId, clientIp
            );
        } catch (DataAccessException ex) {
            throw new AuditWriteException(
                "Failed to write audit record for account " + accountId
                    + " (txn " + transactionId + "): " + ex.getMessage(), ex);
        }
    }
}