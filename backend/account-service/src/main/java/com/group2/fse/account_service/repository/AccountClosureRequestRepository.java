package com.group2.fse.account_service.repository;

import com.group2.fse.account_service.entity.AccountClosureRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountClosureRequestRepository extends JpaRepository<AccountClosureRequest, Long> {
    Page<AccountClosureRequest> findByStatus(String status, Pageable pageable);
    Optional<AccountClosureRequest> findTopByAccount_AccountIdOrderByRequestedAtDesc(Long accountId);
    boolean existsByAccount_AccountIdAndStatus(Long accountId, String status);
    List<AccountClosureRequest> findByAccount_AccountId(Long accountId);
}
