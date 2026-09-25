package com.group2.fse.ledger_service.coordinator;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.group2.fse.ledger_service.audit.AuditWriteException;
import com.group2.fse.ledger_service.audit.CompensationManager;
import com.group2.fse.ledger_service.audit.LedgerAuditWriter;
import com.group2.fse.ledger_service.coordinator.impl.DualWriteCoordinatorImpl;
import com.group2.fse.ledger_service.dto.MutationResult;
import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponse;
import com.group2.fse.ledger_service.service.AccountBalanceService;

class DualWriteCoordinatorTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private LedgerAuditWriter ledgerAuditWriter;

    @Mock
    private CompensationManager compensationManager;

    private DualWriteCoordinator dualWriteCoordinator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dualWriteCoordinator = new DualWriteCoordinatorImpl(accountBalanceService, ledgerAuditWriter, compensationManager);
    }

    @Test
    void testProcessTransfer_Success() {
        TransferRequestDto request = new TransferRequestDto(1L, 2L, new BigDecimal("300.0000"), "TXN-TR-1");

        when(accountBalanceService.executeDebit(1L, new BigDecimal("300.0000"), "TXN-TR-1"))
                .thenReturn(MutationResult.success(1L, "TXN-TR-1", new BigDecimal("1000.0000"), new BigDecimal("700.0000"), new BigDecimal("300.0000")));

        when(accountBalanceService.executeCredit(2L, new BigDecimal("300.0000"), "TXN-TR-1"))
                .thenReturn(MutationResult.success(2L, "TXN-TR-1", new BigDecimal("500.0000"), new BigDecimal("800.0000"), new BigDecimal("300.0000")));

        doNothing().when(ledgerAuditWriter).writeAuditRecord(any(), any(), any(), any(), any(), any(), any(), any(), any());

        TransferResponse response = dualWriteCoordinator.processTransfer(request);

        assertTrue(response.isSuccess());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(new BigDecimal("700.0000"), response.getSourceNewBalance());
        assertEquals(new BigDecimal("800.0000"), response.getDestinationNewBalance());

        verify(accountBalanceService).executeDebit(1L, new BigDecimal("300.0000"), "TXN-TR-1");
        verify(accountBalanceService).executeCredit(2L, new BigDecimal("300.0000"), "TXN-TR-1");
        verify(compensationManager, never()).compensateMutation(any(), any(), any(), any(), any());
    }

    @Test
    void testProcessTransfer_SourceDebitFails() {
        TransferRequestDto request = new TransferRequestDto(1L, 2L, new BigDecimal("2000.0000"), "TXN-TR-2");

        when(accountBalanceService.executeDebit(1L, new BigDecimal("2000.0000"), "TXN-TR-2"))
                .thenReturn(MutationResult.failure(1L, "TXN-TR-2", "Insufficient funds for debit operation."));

        TransferResponse response = dualWriteCoordinator.processTransfer(request);

        assertFalse(response.isSuccess());
        assertEquals("FAILED", response.getStatus());
        assertEquals("Insufficient funds for debit operation.", response.getErrorMessage());

        verify(ledgerAuditWriter, never()).writeAuditRecord(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(accountBalanceService, never()).executeCredit(any(), any(), any());
    }

    @Test
    void testProcessTransfer_SourceAuditFails_CompensatesSourceDebit() {
        TransferRequestDto request = new TransferRequestDto(1L, 2L, new BigDecimal("100.0000"), "TXN-TR-3");

        when(accountBalanceService.executeDebit(1L, new BigDecimal("100.0000"), "TXN-TR-3"))
                .thenReturn(MutationResult.success(1L, "TXN-TR-3", new BigDecimal("500.0000"), new BigDecimal("400.0000"), new BigDecimal("100.0000")));

        doThrow(new AuditWriteException("Postgres connection dropped", 1L, 1L, "TXN-TR-3", new RuntimeException()))
                .when(ledgerAuditWriter).writeAuditRecord(any(), eq(1L), eq("TXN-TR-3"), eq("TRANSFER_OUT"), any(), any(), any(), any(), any());

        TransferResponse response = dualWriteCoordinator.processTransfer(request);

        assertFalse(response.isSuccess());
        assertEquals("ROLLED_BACK", response.getStatus());
        verify(compensationManager).compensateMutation(eq(1L), eq("DEBIT"), eq(new BigDecimal("100.0000")), eq("TXN-TR-3"), any());
        verify(accountBalanceService, never()).executeCredit(any(), any(), any());
    }

    @Test
    void testProcessTransfer_DestinationCreditFails_CompensatesSourceDebit() {
        TransferRequestDto request = new TransferRequestDto(1L, 999L, new BigDecimal("100.0000"), "TXN-TR-4");

        when(accountBalanceService.executeDebit(1L, new BigDecimal("100.0000"), "TXN-TR-4"))
                .thenReturn(MutationResult.success(1L, "TXN-TR-4", new BigDecimal("500.0000"), new BigDecimal("400.0000"), new BigDecimal("100.0000")));

        when(accountBalanceService.executeCredit(999L, new BigDecimal("100.0000"), "TXN-TR-4"))
                .thenReturn(MutationResult.failure(999L, "TXN-TR-4", "Account balance record not found."));

        TransferResponse response = dualWriteCoordinator.processTransfer(request);

        assertFalse(response.isSuccess());
        assertEquals("ROLLED_BACK", response.getStatus());
        verify(compensationManager).compensateMutation(eq(1L), eq("DEBIT"), eq(new BigDecimal("100.0000")), eq("TXN-TR-4"), any());
    }

    @Test
    void testProcessTransfer_DestinationAuditFails_CompensatesBoth() {
        TransferRequestDto request = new TransferRequestDto(1L, 2L, new BigDecimal("100.0000"), "TXN-TR-5");

        when(accountBalanceService.executeDebit(1L, new BigDecimal("100.0000"), "TXN-TR-5"))
                .thenReturn(MutationResult.success(1L, "TXN-TR-5", new BigDecimal("500.0000"), new BigDecimal("400.0000"), new BigDecimal("100.0000")));

        when(accountBalanceService.executeCredit(2L, new BigDecimal("100.0000"), "TXN-TR-5"))
                .thenReturn(MutationResult.success(2L, "TXN-TR-5", new BigDecimal("200.0000"), new BigDecimal("300.0000"), new BigDecimal("100.0000")));

        doNothing().when(ledgerAuditWriter).writeAuditRecord(any(), eq(1L), eq("TXN-TR-5"), eq("TRANSFER_OUT"), any(), any(), any(), any(), any());

        doThrow(new AuditWriteException("Postgres disk full", 2L, 2L, "TXN-TR-5", new RuntimeException()))
                .when(ledgerAuditWriter).writeAuditRecord(any(), eq(2L), eq("TXN-TR-5"), eq("TRANSFER_IN"), any(), any(), any(), any(), any());

        TransferResponse response = dualWriteCoordinator.processTransfer(request);

        assertFalse(response.isSuccess());
        assertEquals("ROLLED_BACK", response.getStatus());
        verify(compensationManager).compensateMutation(eq(2L), eq("CREDIT"), eq(new BigDecimal("100.0000")), eq("TXN-TR-5"), any());
        verify(compensationManager).compensateMutation(eq(1L), eq("DEBIT"), eq(new BigDecimal("100.0000")), eq("TXN-TR-5"), any());
    }
}
