package com.group2.fse.ledger_service.audit.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.group2.fse.ledger_service.audit.AuditWriteException;
import com.group2.fse.ledger_service.audit.LedgerAuditWriter;

@Component
public class PostgresLedgerAuditWriter implements LedgerAuditWriter {

    // Do NOT include previous_hash/current_hash -- trg_compute_ledger_hash_chain
    // (03_audit_triggers.sql) fills those in BEFORE INSERT.
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
            // Unchecked on purpose: DualWriteCoordinator's catch block should
            // trigger CompensationManager. Do not swallow this.
            throw new AuditWriteException(
                "Failed to write audit record for account " + accountId
                    + " (txn " + transactionId + "): " + ex.getMessage(), ex);
        }
    }
}