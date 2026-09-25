package com.group2.fse.ledger_service.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.group2.fse.ledger_service.exception.DualWriteAuditException;

/**
 * Service responsible for executing simultaneous dual-write updates to the
 * immutable PostgreSQL forensic audit store (FSE-304).
 *
 * PostgreSQL triggers automatically calculate the SHA-256 tamper-evident hash chain.
 * Any write failure triggers a DualWriteAuditException, rolling back the Oracle
 * master ledger transaction (FSE-305).
 */
@Service
public class DualWriteLedgerAuditService {

    private static final Logger log = LoggerFactory.getLogger(DualWriteLedgerAuditService.class);

    public static final String INSERT_AUDIT_SQL = """
        INSERT INTO audit_store.ledger_mutation_audit
        (transaction_id, account_id, transaction_type, amount, old_balance, new_balance, actor_id, client_ip)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private final JdbcTemplate auditJdbcTemplate;

    public DualWriteLedgerAuditService(@Qualifier("auditJdbcTemplate") JdbcTemplate auditJdbcTemplate) {
        this.auditJdbcTemplate = auditJdbcTemplate;
    }

    /**
     * Executes the secondary write to PostgreSQL immutable audit store.
     * The PostgreSQL BEFORE INSERT trigger (trg_compute_hash_chain) automatically
     * computes the SHA-256 tamper-evident hash chain across the ledger records.
     *
     * If the write fails, a DualWriteAuditException is thrown to trigger
     * automated transaction rollback in the Oracle primary datasource (FSE-305).
     */
    public void recordMutationAudit(
            Long transactionId,
            Long accountId,
            String transactionType,
            BigDecimal amount,
            BigDecimal oldBalance,
            BigDecimal newBalance,
            Long actorId,
            String clientIp) {

        log.info("Initiating dual-write audit record: txnId={}, accountId={}, type={}, amount={}",
                transactionId, accountId, transactionType, amount);

        try {
            int rowsAffected = auditJdbcTemplate.update(
                    INSERT_AUDIT_SQL,
                    transactionId,
                    accountId,
                    transactionType,
                    amount,
                    oldBalance,
                    newBalance,
                    actorId,
                    clientIp != null ? clientIp : "127.0.0.1"
            );

            if (rowsAffected != 1) {
                throw new DualWriteAuditException("Audit record insertion affected " + rowsAffected + " rows, expected 1.");
            }

            log.info("Successfully persisted immutable audit record in PostgreSQL for txnId={}", transactionId);

        } catch (DataAccessException ex) {
            log.error("CRITICAL: Failed to write to PostgreSQL audit store for txnId={}. Error: {}",
                    transactionId, ex.getMessage(), ex);
            throw new DualWriteAuditException(
                    "PostgreSQL audit store write failed for transaction " + transactionId + ": " + ex.getMessage(),
                    ex
            );
        }
    }
}
