package com.group2.fse.ledger_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "BALANCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Builder.Default
    @Column(name = "available_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO.setScale(4);

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.availableBalance == null) {
            this.availableBalance = BigDecimal.ZERO.setScale(4);
        } else {
            this.availableBalance = this.availableBalance.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    /**
     * Domain invariant check: ensures balance never drops below zero
     */
    public boolean canDebit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return this.availableBalance.compareTo(amount) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return balanceId != null && Objects.equals(balanceId, balance.balanceId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
