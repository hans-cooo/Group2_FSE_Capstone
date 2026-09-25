package com.group2.fse.ledger_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "TRANSACTION_AUDIT")
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "transaction_type", length = 30, nullable = false)
    private String transactionType;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "old_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal oldBalance;

    @Column(name = "new_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal newBalance;

    @Builder.Default
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
        if (amount != null) {
            this.amount = this.amount.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        if (oldBalance != null) {
            this.oldBalance = this.oldBalance.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        if (newBalance != null) {
            this.newBalance = this.newBalance.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionAudit that = (TransactionAudit) o;
        return auditId != null && Objects.equals(auditId, that.auditId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
