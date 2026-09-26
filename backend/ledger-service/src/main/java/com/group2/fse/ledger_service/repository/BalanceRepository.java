package com.group2.fse.ledger_service.repository;

import com.group2.fse.ledger_service.entity.Balance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT b FROM Balance b WHERE b.account.accountId = :accountId")
    Optional<Balance> findByAccountId(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    Optional<Balance> findByAccount_AccountId(Long accountId);

    /**
     * Non-locking balance read query for fast read-only queries and cache lookups.
     */
    @Query("SELECT b FROM Balance b WHERE b.account.accountId = :accountId")
    Optional<Balance> findReadOnlyByAccountId(@Param("accountId") Long accountId);
}