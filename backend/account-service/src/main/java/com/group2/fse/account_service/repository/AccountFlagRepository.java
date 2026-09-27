package com.group2.fse.account_service.repository;

import com.group2.fse.account_service.entity.AccountFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountFlagRepository extends JpaRepository<AccountFlag, Long> {
    List<AccountFlag> findByAccount_AccountId(Long accountId);
    List<AccountFlag> findByAccount_AccountIdAndStatus(Long accountId, String status);
    boolean existsByAccount_AccountIdAndStatus(Long accountId, String status);
}
