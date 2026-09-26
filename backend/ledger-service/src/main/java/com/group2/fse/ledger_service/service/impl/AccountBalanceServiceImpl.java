package com.group2.fse.ledger_service.service.impl;

import com.group2.fse.ledger_service.dto.BalanceResponseDto;
import com.group2.fse.ledger_service.dto.DebitCreditRequestDto;
import com.group2.fse.ledger_service.dto.DebitCreditResponseDto;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponseDto;
import com.group2.fse.ledger_service.entity.Account;
import com.group2.fse.ledger_service.entity.Balance;
import com.group2.fse.ledger_service.entity.Transaction;
import com.group2.fse.ledger_service.entity.User;
import com.group2.fse.ledger_service.exception.AccountNotFoundException;
import com.group2.fse.ledger_service.exception.InsufficientFundsException;
import com.group2.fse.ledger_service.exception.InvalidTransactionException;
import com.group2.fse.ledger_service.repository.BalanceRepository;
import com.group2.fse.ledger_service.repository.TransactionRepository;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import com.group2.fse.ledger_service.service.DualWriteLedgerAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Core Balance Mutation Engine coordinating Oracle Master SoR and PostgreSQL Forensic Audit Store.
 * Implements:
 * - FSE-302: Row-level pessimistic locking via BalanceRepository.findByAccountId with timeout hints
 * - FSE-303: Zero-balance floor and atomic debit/credit operations
 * - FSE-304: Simultaneous dual-write updates across Oracle and PostgreSQL
 * - FSE-305: Automated rollback via @Transactional(rollbackFor = Exception.class) on audit failure
 * - FSE-306: Deadlock-free ordered pessimistic row locking for multi-account atomic transfers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final DualWriteLedgerAuditService dualWriteLedgerAuditService;

    // =========================================================================
    // Legacy methods preserved for backwards compatibility with existing tests
    // =========================================================================

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

    // =========================================================================
    // Enterprise REST API Service Implementations (Exceptions on domain errors)
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResponseDto executeTransfer(TransferRequestDto request) {
        return executeTransfer(request, null, "127.0.0.1");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResponseDto executeTransfer(TransferRequestDto request, Long actorId, String clientIp) {
        Long srcId = request.getSourceAccountId();
        Long dstId = request.getDestinationAccountId();
        BigDecimal amount = request.getAmount();

        if (srcId == null || dstId == null) {
            throw new InvalidTransactionException("Source and destination account IDs are required.");
        }
        if (srcId.equals(dstId)) {
            throw new InvalidTransactionException("Source and destination accounts must be distinct accounts.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero.");
        }

        // Deadlock-free ordered lock acquisition: min(src, dst) then max(src, dst)
        Long firstId = Math.min(srcId, dstId);
        Long secondId = Math.max(srcId, dstId);

        Balance firstBalance = balanceRepository.findByAccountId(firstId)
                .or(() -> balanceRepository.findById(firstId))
                .orElseThrow(() -> new AccountNotFoundException(firstId));

        Balance secondBalance = balanceRepository.findByAccountId(secondId)
                .or(() -> balanceRepository.findById(secondId))
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Balance sourceBalance = srcId.equals(firstId) ? firstBalance : secondBalance;
        Balance destinationBalance = dstId.equals(firstId) ? firstBalance : secondBalance;

        BigDecimal srcPrev = sourceBalance.getAvailableBalance();
        if (srcPrev.compareTo(amount) < 0) {
            throw new InsufficientFundsException(srcId, srcPrev, amount);
        }

        BigDecimal srcNew = srcPrev.subtract(amount);
        BigDecimal dstPrev = destinationBalance.getAvailableBalance();
        BigDecimal dstNew = dstPrev.add(amount);

        sourceBalance.setAvailableBalance(srcNew);
        destinationBalance.setAvailableBalance(dstNew);
        balanceRepository.save(sourceBalance);
        balanceRepository.save(destinationBalance);

        String ref = request.getReferenceNo();
        String refDebit = ref.length() > 33 ? ref.substring(0, 33) + "-D" : ref + "-D";
        String refCredit = ref.length() > 33 ? ref.substring(0, 33) + "-C" : ref + "-C";

        // Oracle Master SoR: Save paired transaction records
        Account srcAccount = sourceBalance.getAccount() != null ? sourceBalance.getAccount() : Account.builder().accountId(srcId).build();
        Account dstAccount = destinationBalance.getAccount() != null ? destinationBalance.getAccount() : Account.builder().accountId(dstId).build();
        User approvedByUser = actorId != null ? User.builder().userId(actorId).build() : null;

        Transaction debitTxn = Transaction.builder()
                .account(srcAccount)
                .referenceNo(refDebit)
                .transactionType("DEBIT")
                .amount(amount)
                .previousBalance(srcPrev)
                .newBalance(srcNew)
                .status("COMPLETED")
                .approvedBy(approvedByUser)
                .build();
        Transaction savedDebitTxn = transactionRepository.save(debitTxn);

        Transaction creditTxn = Transaction.builder()
                .account(dstAccount)
                .referenceNo(refCredit)
                .transactionType("CREDIT")
                .amount(amount)
                .previousBalance(dstPrev)
                .newBalance(dstNew)
                .status("COMPLETED")
                .approvedBy(approvedByUser)
                .build();
        Transaction savedCreditTxn = transactionRepository.save(creditTxn);

        // PostgreSQL Forensic Audit Store: Simultaneous dual-write for both legs
        dualWriteLedgerAuditService.recordMutationAudit(
                savedDebitTxn.getTransactionId() != null ? savedDebitTxn.getTransactionId() : 0L,
                srcId, "DEBIT", amount, srcPrev, srcNew, actorId, clientIp
        );
        dualWriteLedgerAuditService.recordMutationAudit(
                savedCreditTxn.getTransactionId() != null ? savedCreditTxn.getTransactionId() : 0L,
                dstId, "CREDIT", amount, dstPrev, dstNew, actorId, clientIp
        );

        log.info("Atomic transfer completed: ref={}, src={} ({} -> {}), dst={} ({} -> {}), amount={}",
                ref, srcId, srcPrev, srcNew, dstId, dstPrev, dstNew, amount);

        return TransferResponseDto.builder()
                .transferReference(ref)
                .sourceAccountId(srcId)
                .destinationAccountId(dstId)
                .amount(amount)
                .sourcePreviousBalance(srcPrev)
                .sourceNewBalance(srcNew)
                .destinationPreviousBalance(dstPrev)
                .destinationNewBalance(dstNew)
                .status("COMPLETED")
                .timestamp(Instant.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DebitCreditResponseDto mutateDebit(DebitCreditRequestDto request, Long actorId, String clientIp) {
        Long accountId = request.getAccountId();
        BigDecimal amount = request.getAmount();
        String ref = request.getReferenceNo();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Debit amount must be greater than zero.");
        }

        Balance balance = balanceRepository.findByAccountId(accountId)
                .or(() -> balanceRepository.findById(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal currentBalance = balance.getAvailableBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, currentBalance, amount);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        balance.setAvailableBalance(newBalance);
        balanceRepository.save(balance);

        Account account = balance.getAccount() != null ? balance.getAccount() : Account.builder().accountId(accountId).build();
        Transaction txn = Transaction.builder()
                .account(account)
                .referenceNo(ref)
                .transactionType("DEBIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .approvedBy(actorId != null ? User.builder().userId(actorId).build() : null)
                .build();
        Transaction savedTxn = transactionRepository.save(txn);
        Long txnId = savedTxn != null ? savedTxn.getTransactionId() : 0L;

        dualWriteLedgerAuditService.recordMutationAudit(
                txnId, accountId, "DEBIT", amount, currentBalance, newBalance, actorId, clientIp
        );

        return DebitCreditResponseDto.builder()
                .transactionId(txnId)
                .accountId(accountId)
                .transactionType("DEBIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .referenceNo(ref)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DebitCreditResponseDto mutateCredit(DebitCreditRequestDto request, Long actorId, String clientIp) {
        Long accountId = request.getAccountId();
        BigDecimal amount = request.getAmount();
        String ref = request.getReferenceNo();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Credit amount must be greater than zero.");
        }

        Balance balance = balanceRepository.findByAccountId(accountId)
                .or(() -> balanceRepository.findById(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal currentBalance = balance.getAvailableBalance();
        BigDecimal newBalance = currentBalance.add(amount);
        balance.setAvailableBalance(newBalance);
        balanceRepository.save(balance);

        Account account = balance.getAccount() != null ? balance.getAccount() : Account.builder().accountId(accountId).build();
        Transaction txn = Transaction.builder()
                .account(account)
                .referenceNo(ref)
                .transactionType("CREDIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .approvedBy(actorId != null ? User.builder().userId(actorId).build() : null)
                .build();
        Transaction savedTxn = transactionRepository.save(txn);
        Long txnId = savedTxn != null ? savedTxn.getTransactionId() : 0L;

        dualWriteLedgerAuditService.recordMutationAudit(
                txnId, accountId, "CREDIT", amount, currentBalance, newBalance, actorId, clientIp
        );

        return DebitCreditResponseDto.builder()
                .transactionId(txnId)
                .accountId(accountId)
                .transactionType("CREDIT")
                .amount(amount)
                .previousBalance(currentBalance)
                .newBalance(newBalance)
                .status("COMPLETED")
                .referenceNo(ref)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponseDto getAccountBalance(Long accountId) {
        Balance balance = balanceRepository.findReadOnlyByAccountId(accountId)
                .or(() -> balanceRepository.findById(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return BalanceResponseDto.builder()
                .accountId(accountId)
                .currency("PHP")
                .availableBalance(balance.getAvailableBalance())
                .asOfTimestamp(Instant.now())
                .isCached(false)
                .build();
    }
}