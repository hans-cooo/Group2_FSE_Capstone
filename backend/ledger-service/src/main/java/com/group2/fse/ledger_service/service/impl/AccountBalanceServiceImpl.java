package com.group2.fse.ledger_service.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.entity.Balance;
import com.group2.fse.ledger_service.repository.BalanceRepository;
import com.group2.fse.ledger_service.service.AccountBalanceService;

@Service
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private final BalanceRepository balanceRepository;

    public AccountBalanceServiceImpl(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    @Override
    @Transactional
    public MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MutationResult.failure(accountId, referenceNo, "Debit amount must be greater than zero.");
        }

        // Fetch balance record (Hans will add @Lock method in BalanceRepository)
        Optional<Balance> balanceOpt = balanceRepository.findById(accountId);
        if (balanceOpt.isEmpty()) {
            return MutationResult.failure(accountId, referenceNo, "Account balance record not found.");
        }

        Balance balance = balanceOpt.get();
        BigDecimal currentBalance = balance.getAvailableBalance();

        if (currentBalance.compareTo(amount) < 0) {
            return MutationResult.failure(accountId, referenceNo, "Insufficient funds for debit operation.");
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        balance.setAvailableBalance(newBalance);
        balanceRepository.save(balance);

        return MutationResult.success(accountId, referenceNo, currentBalance, newBalance, amount);
    }

    @Override
    @Transactional
    public MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MutationResult.failure(accountId, referenceNo, "Credit amount must be greater than zero.");
        }

        Optional<Balance> balanceOpt = balanceRepository.findById(accountId);
        if (balanceOpt.isEmpty()) {
            return MutationResult.failure(accountId, referenceNo, "Account balance record not found.");
        }

        Balance balance = balanceOpt.get();
        BigDecimal currentBalance = balance.getAvailableBalance();
        BigDecimal newBalance = currentBalance.add(amount);

        balance.setAvailableBalance(newBalance);
        balanceRepository.save(balance);

        return MutationResult.success(accountId, referenceNo, currentBalance, newBalance, amount);
    }
}