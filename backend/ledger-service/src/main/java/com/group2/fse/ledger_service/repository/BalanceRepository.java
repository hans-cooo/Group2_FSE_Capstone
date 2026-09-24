package com.group2.fse.ledger_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.group2.fse.ledger_service.entity.Balance;

import jakarta.persistence.LockModeType;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.account.accountId = :accountId")
    Optional<Balance> findByAccountId(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Balance> findByAccount_AccountId(Long accountId);

}