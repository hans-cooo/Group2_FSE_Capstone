package com.group2.fse.audit_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ledger_mutation_audit", schema = "audit_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "old_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal oldBalance;

    @Column(name = "new_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal newBalance;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "current_hash")
    private String currentHash;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
}