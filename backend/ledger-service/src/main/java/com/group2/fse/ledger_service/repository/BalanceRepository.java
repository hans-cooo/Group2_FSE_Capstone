package com.group2.fse.ledger_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.group2.fse.ledger_service.entity.Balance;

import jakarta.persistence.LockModeType;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Balance> findByAccountId(Long accountId);

}