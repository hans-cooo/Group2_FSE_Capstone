package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;

import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.service.AccountBalanceService;

class CompensationManagerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    private CompensationManager compensationManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compensationManager = new CompensationManager(accountBalanceService);
    }

    @Test
    void compensate_originalDebit_firesCredit() {
        MutationResult original = MutationResult.success(
            1L, "REF-001", new BigDecimal("1000.0000"), new BigDecimal("800.0000"), new BigDecimal("200.0000"));

        MutationResult reversal = MutationResult.success(
            1L, "COMP-REF-001", new BigDecimal("800.0000"), new BigDecimal("1000.0000"), new BigDecimal("200.0000"));

        when(accountBalanceService.executeCredit(eq(1L), eq(new BigDecimal("200.0000")), eq("COMP-REF-001")))
            .thenReturn(reversal);

        MutationResult result = compensationManager.compensate(original, "DEBIT");

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("1000.0000"), result.getNewBalance());
    }

    @Test
    void compensate_nullOriginal_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> compensationManager.compensate(null, "DEBIT"));
    }

    @Test
    void compensate_failedOriginal_throws() {
        MutationResult failed = MutationResult.failure(1L, "REF-002", "insufficient funds");
        assertThrows(IllegalArgumentException.class,
            () -> compensationManager.compensate(failed, "DEBIT"));
    }
}