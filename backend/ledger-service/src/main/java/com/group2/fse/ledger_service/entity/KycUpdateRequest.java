package com.group2.fse.ledger_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "KYC_UPDATE_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kyc_request_id")
    private Long kycRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_id", nullable = false)
    private Kyc kyc;

    @Column(name = "new_first_name", length = 100)
    private String newFirstName;

    @Column(name = "new_middle_initial", length = 10)
    private String newMiddleInitial;

    @Column(name = "new_last_name", length = 100)
    private String newLastName;

    @Column(name = "new_address", length = 255)
    private String newAddress;

    @Column(name = "new_mobile_number", length = 30)
    private String newMobileNumber;

    @Column(name = "new_civil_status", length = 50)
    private String newCivilStatus;

    @Column(name = "new_occupation", length = 100)
    private String newOccupation;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

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
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KycUpdateRequest that = (KycUpdateRequest) o;
        return kycRequestId != null && Objects.equals(kycRequestId, that.kycRequestId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
