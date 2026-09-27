package com.group2.fse.account_service.repository;

import com.group2.fse.account_service.entity.KycUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycUpdateRequestRepository extends JpaRepository<KycUpdateRequest, Long> {
    Page<KycUpdateRequest> findByStatus(String status, Pageable pageable);
    List<KycUpdateRequest> findByKyc_KycId(Long kycId);
    List<KycUpdateRequest> findByKyc_Customer_CustomerId(Long customerId);
}
