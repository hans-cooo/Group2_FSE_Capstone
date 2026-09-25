package com.group2.fse.ledger_service.audit;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.group2.fse.ledger_service.audit.impl.CompensationManagerImpl;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.service.AccountBalanceService;

class CompensationManagerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    private CompensationManager compensationManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        compensationManager = new CompensationManagerImpl(accountBalanceService);
    }

    @Test
    void testCompensateDebit_ExecutesCredit() {
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.0000");
        String originalRef = "REF-ORIG-1";
        String expectedCompRef = "COMP-" + originalRef;

        when(accountBalanceService.executeCredit(eq(accountId), eq(amount), eq(expectedCompRef)))
                .thenReturn(MutationResult.success(accountId, expectedCompRef, new BigDecimal("400.0000"), new BigDecimal("500.0000"), amount));

        MutationResult result = compensationManager.compensateMutation(accountId, "DEBIT", amount, originalRef, "Audit store failure");

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("500.0000"), result.getNewBalance());
        verify(accountBalanceService).executeCredit(accountId, amount, expectedCompRef);
    }

    @Test
    void testCompensateCredit_ExecutesDebit() {
        Long accountId = 2L;
        BigDecimal amount = new BigDecimal("50.0000");
        String originalRef = "REF-ORIG-2";
        String expectedCompRef = "COMP-" + originalRef;

        when(accountBalanceService.executeDebit(eq(accountId), eq(amount), eq(expectedCompRef)))
                .thenReturn(MutationResult.success(accountId, expectedCompRef, new BigDecimal("250.0000"), new BigDecimal("200.0000"), amount));

        MutationResult result = compensationManager.compensateMutation(accountId, "CREDIT", amount, originalRef, "Downstream failure");

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("200.0000"), result.getNewBalance());
        verify(accountBalanceService).executeDebit(accountId, amount, expectedCompRef);
    }
}
