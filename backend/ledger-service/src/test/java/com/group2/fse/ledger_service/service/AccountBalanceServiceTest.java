package com.group2.fse.ledger_service.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.entity.Balance;
import com.group2.fse.ledger_service.repository.BalanceRepository;
import com.group2.fse.ledger_service.service.impl.AccountBalanceServiceImpl;

class AccountBalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private AccountBalanceServiceImpl balanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecuteDebit_Success() {
        Balance balance = new Balance();
        balance.setAvailableBalance(new BigDecimal("1000.0000"));

        when(balanceRepository.findById(1L)).thenReturn(Optional.of(balance));

        MutationResult result = balanceService.executeDebit(1L, new BigDecimal("200.0000"), "REF-001");

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("1000.0000"), result.getPreviousBalance());
        assertEquals(new BigDecimal("800.0000"), result.getNewBalance());
        verify(balanceRepository, times(1)).save(balance);
    }

    @Test
    void testExecuteDebit_InsufficientBalance() {
        Balance balance = new Balance();
        balance.setAvailableBalance(new BigDecimal("100.0000"));

        when(balanceRepository.findById(1L)).thenReturn(Optional.of(balance));

        MutationResult result = balanceService.executeDebit(1L, new BigDecimal("200.0000"), "REF-002");

        assertFalse(result.isSuccess());
        assertEquals("Insufficient funds for debit operation.", result.getErrorMessage());
        verify(balanceRepository, never()).save(any());
    }
}