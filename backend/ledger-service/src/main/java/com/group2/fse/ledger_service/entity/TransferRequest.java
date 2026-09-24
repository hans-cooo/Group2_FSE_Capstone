package com.group2.fse.ledger_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "TRANSFER_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_request_id")
    private Long transferRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "reference_no", length = 36, nullable = false, unique = true)
    private String referenceNo;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Transient
    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (version == null) {
            version = 0L;
        }
        if (amount != null) {
            this.amount = this.amount.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferRequest that = (TransferRequest) o;
        return transferRequestId != null && Objects.equals(transferRequestId, that.transferRequestId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
