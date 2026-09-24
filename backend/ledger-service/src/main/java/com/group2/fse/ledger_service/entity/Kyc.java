package com.group2.fse.ledger_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "KYC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kyc_id")
    private Long kycId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "middle_initial", length = 10)
    private String middleInitial;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "civil_status", length = 50)
    private String civilStatus;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "mobile_number", length = 30, nullable = false)
    private String mobileNumber;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "VERIFIED";

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "VERIFIED";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kyc kyc = (Kyc) o;
        return kycId != null && Objects.equals(kycId, kyc.kycId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
