package com.group2.fse.audit_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.fse.audit_service.entity.TransactionAudit;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {

    Page<TransactionAudit> findByAccountIdAndEventTimestampBetween(
            Long accountId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<TransactionAudit> findByAccountId(Long accountId, Pageable pageable);

    Optional<TransactionAudit> findByTransactionId(Long transactionId);

    List<TransactionAudit> findByAccountIdOrderByAuditIdAsc(Long accountId);
}