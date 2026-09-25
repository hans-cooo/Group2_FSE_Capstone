package com.group2.fse.ledger_service.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JPA Entities Domain Mapping Tests (FSE-204)")
class EntityMappingTest {

    @Test
    @DisplayName("Verify Balance entity invariant and fixed-point precision (scale 4)")
    void testBalanceEntityInvariants() {
        Account account = Account.builder()
                .accountId(1L)
                .accountNumber("ACC_10000001")
                .accountType("SAVINGS")
                .currency("PHP")
                .status("ACTIVE")
                .build();

        Balance balance = Balance.builder()
                .balanceId(1L)
                .account(account)
                .availableBalance(new BigDecimal("50000.0000"))
                .updatedAt(LocalDateTime.now())
                .build();

        assertNotNull(balance);
        assertEquals(0, new BigDecimal("50000.0000").compareTo(balance.getAvailableBalance()));
        assertEquals(4, balance.getAvailableBalance().scale());

        // Invariant: canDebit check
        assertTrue(balance.canDebit(new BigDecimal("25000.0000")));
        assertTrue(balance.canDebit(new BigDecimal("50000.0000")));
        assertFalse(balance.canDebit(new BigDecimal("50000.0001")));
        assertFalse(balance.canDebit(new BigDecimal("-100.0000")));
        assertFalse(balance.canDebit(null));
    }

    @Test
    @DisplayName("Verify Transaction entity mapping with unique reference number")
    void testTransactionEntityMapping() {
        Account account = Account.builder().accountId(1L).build();

        Transaction txn = Transaction.builder()
                .transactionId(101L)
                .account(account)
                .referenceNo("REF-20260924-001")
                .transactionType("TRANSFER_DEBIT")
                .amount(new BigDecimal("5000.0000"))
                .previousBalance(new BigDecimal("50000.0000"))
                .newBalance(new BigDecimal("45000.0000"))
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .build();

        assertNotNull(txn);
        assertEquals("REF-20260924-001", txn.getReferenceNo());
        assertEquals("TRANSFER_DEBIT", txn.getTransactionType());
        assertEquals(0, new BigDecimal("45000.0000").compareTo(txn.getNewBalance()));
    }

    @Test
    @DisplayName("Verify TransferRequest entity with idempotency key and versioning")
    void testTransferRequestEntityMapping() {
        Account src = Account.builder().accountId(1L).build();
        Account dst = Account.builder().accountId(2L).build();

        TransferRequest req = TransferRequest.builder()
                .transferRequestId(501L)
                .sourceAccount(src)
                .destinationAccount(dst)
                .amount(new BigDecimal("1000.0000"))
                .idempotencyKey("idemp-uuid-12345")
                .referenceNo("REF-TRF-001")
                .status("PENDING")
                .version(0L)
                .requestedAt(LocalDateTime.now())
                .build();

        assertNotNull(req);
        assertEquals("idemp-uuid-12345", req.getIdempotencyKey());
        assertEquals("REF-TRF-001", req.getReferenceNo());
        assertEquals(0L, req.getVersion());
    }

    @Test
    @DisplayName("Verify Customer and KYC entity mapping")
    void testCustomerAndKycMapping() {
        Customer customer = Customer.builder()
                .customerId(10L)
                .username("john_doe")
                .passwordHash("$2a$10$hashedpassword")
                .email("john@example.com")
                .kycStatus("VERIFIED")
                .build();

        Kyc kyc = Kyc.builder()
                .kycId(20L)
                .customer(customer)
                .firstName("John")
                .middleInitial("D")
                .lastName("Doe")
                .address("Makati City")
                .mobileNumber("+639171234567")
                .status("VERIFIED")
                .build();

        assertNotNull(customer);
        assertNotNull(kyc);
        assertEquals("john_doe", kyc.getCustomer().getUsername());
        assertEquals("VERIFIED", kyc.getStatus());
    }

    @Test
    @DisplayName("Verify AccountClosureRequest entity mapping and lifecycle")
    void testAccountClosureRequestEntityMapping() {
        Account account = Account.builder()
                .accountId(1L)
                .accountNumber("ACC_10000001")
                .status("ACTIVE")
                .build();

        User admin = User.builder()
                .userId(1L)
                .username("admin")
                .build();

        AccountClosureRequest request = AccountClosureRequest.builder()
                .closureRequestId(99L)
                .account(account)
                .reason("No longer needed")
                .status("PENDING")
                .approvedBy(admin)
                .requestedAt(LocalDateTime.now())
                .build();

        assertNotNull(request);
        assertEquals(99L, request.getClosureRequestId());
        assertEquals("PENDING", request.getStatus());
        assertEquals("No longer needed", request.getReason());
        assertEquals(1L, request.getAccount().getAccountId());
        assertEquals("admin", request.getApprovedBy().getUsername());
    }
}
