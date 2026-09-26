package com.group2.fse.auth_service.repository;

import com.group2.fse.auth_service.entity.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycRepository extends JpaRepository<Kyc, Long> {

    Optional<Kyc> findByCustomerCustomerId(Long customerId);
}
