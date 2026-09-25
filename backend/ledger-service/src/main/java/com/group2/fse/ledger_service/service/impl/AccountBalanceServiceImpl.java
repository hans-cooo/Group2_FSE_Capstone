package com.group2.fse.ledger_service.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.entity.Account;
import com.group2.fse.ledger_service.entity.Balance;
import com.group2.fse.ledger_service.entity.Transaction;
import com.group2.fse.ledger_service.entity.User;
import com.group2.fse.ledger_service.repository.BalanceRepository;
import com.group2.fse.ledger_service.repository.TransactionRepository;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import com.group2.fse.ledger_service.service.DualWriteLedgerAuditService;

/**
 * Core Balance Mutation Engine coordinating Oracle Master SoR and PostgreSQL Forensic Audit Store.
 * Implements:
 * - FSE-302: Row-level pessimistic locking via BalanceRepository.findByAccountId
 * - FSE-303: Zero-balance floor and atomic debit/credit operations
 * - FSE-304: Simultaneous dual-write updates across Oracle and PostgreSQL
 * - FSE-305: Automated rollback via @Transactional(rollbackFor = Exception.class) on audit failure
 */
@Service
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private static final Logger log = LoggerFactory.getLogger(AccountBalanceServiceImpl.class);

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final DualWriteLedgerAuditService dualWriteLedgerAuditService;

    public AccountBalanceServiceImpl(
            BalanceRepository balanceRepository,
            TransactionRepository transactionRepository,
            DualWriteLedgerAuditService dualWriteLedgerAuditService) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.dualWriteLedgerAuditService = dualWriteLedgerAuditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo) {
        return executeDebit(accountId, amount, referenceNo, null, "127.0.0.1");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationResult executeDebit(Long accountId, BigDecimal amount, String referenceNo, Long actorId, String clientIp) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MutationResult.failure(accountId, referenceNo, "Debit amount must be greater than zero.");
        }

        // Fetch balance record with pessimistic write lock (FSE-302)
        Optional<Balance> balanceOpt = balanceRepository.findByAccountId(accountId);
        if (balanceOpt.isEmpty()) {
            balanceOpt = balanceRepository.findById(accountId);
        }
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

        // 1. Oracle Master: Record transaction journal entry
        Account account = balance.getAccount();
        if (account == null) {
            account = Account.builder().accountId(accountId).build();
        }

        Transaction txn = Transaction.builder()
                .account(account)
                .referenceNo(referenceNo)
                .transactionType("DEBIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .approvedBy(actorId != null ? User.builder().userId(actorId).build() : null)
                .build();

        Transaction savedTxn = transactionRepository.save(txn);
        Long txnId = savedTxn != null ? savedTxn.getTransactionId() : null;

        // 2. PostgreSQL Secondary Store: Simultaneous dual-write audit update (FSE-304)
        // If this throws, @Transactional triggers rollback of Oracle mutation (FSE-305)
        dualWriteLedgerAuditService.recordMutationAudit(
                txnId != null ? txnId : 0L,
                accountId,
                "DEBIT",
                amount,
                currentBalance,
                newBalance,
                actorId,
                clientIp
        );

        return MutationResult.success(accountId, referenceNo, txnId, currentBalance, newBalance, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo) {
        return executeCredit(accountId, amount, referenceNo, null, "127.0.0.1");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationResult executeCredit(Long accountId, BigDecimal amount, String referenceNo, Long actorId, String clientIp) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return MutationResult.failure(accountId, referenceNo, "Credit amount must be greater than zero.");
        }

        Optional<Balance> balanceOpt = balanceRepository.findByAccountId(accountId);
        if (balanceOpt.isEmpty()) {
            balanceOpt = balanceRepository.findById(accountId);
        }
        if (balanceOpt.isEmpty()) {
            return MutationResult.failure(accountId, referenceNo, "Account balance record not found.");
        }

        Balance balance = balanceOpt.get();
        BigDecimal currentBalance = balance.getAvailableBalance();
        BigDecimal newBalance = currentBalance.add(amount);

        balance.setAvailableBalance(newBalance);
        balanceRepository.save(balance);

        Account account = balance.getAccount();
        if (account == null) {
            account = Account.builder().accountId(accountId).build();
        }

        Transaction txn = Transaction.builder()
                .account(account)
                .referenceNo(referenceNo)
                .transactionType("CREDIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .approvedBy(actorId != null ? User.builder().userId(actorId).build() : null)
                .build();

        Transaction savedTxn = transactionRepository.save(txn);
        Long txnId = savedTxn != null ? savedTxn.getTransactionId() : null;

        // PostgreSQL Secondary Store: Simultaneous dual-write audit update (FSE-304)
        dualWriteLedgerAuditService.recordMutationAudit(
                txnId != null ? txnId : 0L,
                accountId,
                "CREDIT",
                amount,
                currentBalance,
                newBalance,
                actorId,
                clientIp
        );

        return MutationResult.success(accountId, referenceNo, txnId, currentBalance, newBalance, amount);
    }
}