package com.group2.fse.ledger_service.integration;

import com.group2.fse.ledger_service.dto.TransferRequestDto;
import com.group2.fse.ledger_service.dto.TransferResponseDto;
import com.group2.fse.ledger_service.entity.Balance;
import com.group2.fse.ledger_service.repository.BalanceRepository;
import com.group2.fse.ledger_service.service.AccountBalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task: FSE-306 / Phase 6 Stress Test
 * High-concurrency integration test executing simultaneous cross-transfers (1 -> 2 and 2 -> 1)
 * across 50 concurrent threads to prove:
 * 1. Deadlock-free ordered lock acquisition [min(src, dst), max(src, dst)].
 * 2. Total invariant preservation: Sum(Balance_A + Balance_B) = Constant C.
 * 3. Zero balance leakage or race conditions.
 */
@SpringBootTest
@DisplayName("High-Concurrency Multi-Threaded Cross-Transfer Stress Tests (Phase 6 / FSE-306)")
class ConcurrentTransferIntegrationTest {

    @Autowired
    private AccountBalanceService accountBalanceService;

    @Autowired
    private BalanceRepository balanceRepository;

    private static final int CONCURRENT_THREADS = 50;
    private static final Long ACCOUNT_1_ID = 1L;
    private static final Long ACCOUNT_2_ID = 2L;
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("10.0000");

    @Test
    @DisplayName("Should execute 50 simultaneous cross-transfers without deadlocks while preserving balance invariant")
    void shouldExecuteConcurrentCrossTransfersWithoutDeadlocks() throws InterruptedException {
        // Step 1: Capture initial baseline balances
        Balance initialBalance1 = balanceRepository.findReadOnlyByAccountId(ACCOUNT_1_ID)
                .orElseThrow(() -> new IllegalStateException("Account 1 balance not found"));
        Balance initialBalance2 = balanceRepository.findReadOnlyByAccountId(ACCOUNT_2_ID)
                .orElseThrow(() -> new IllegalStateException("Account 2 balance not found"));

        BigDecimal initialB1 = initialBalance1.getAvailableBalance();
        BigDecimal initialB2 = initialBalance2.getAvailableBalance();
        BigDecimal expectedTotalInvariant = initialB1.add(initialB2);

        System.out.printf("Starting concurrency stress test with %d threads:%n", CONCURRENT_THREADS);
        System.out.printf("  Initial Account 1: %s%n", initialB1);
        System.out.printf("  Initial Account 2: %s%n", initialB2);
        System.out.printf("  Expected Total Balance Invariant: %s%n", expectedTotalInvariant);

        // Step 2: Prepare 50 concurrent workers (25 cross transfers 1 -> 2, 25 cross transfers 2 -> 1)
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final boolean forward = (i % 2 == 0); // Alternate directions to maximize deadlock contention
            final String referenceNo = "STRESS-" + (forward ? "F-" : "R-") + UUID.randomUUID().toString().substring(0, 8);

            executor.submit(() -> {
                try {
                    // Block until all 50 threads are created and ready to fire simultaneously
                    startLatch.await();

                    TransferRequestDto request;
                    if (forward) {
                        request = TransferRequestDto.builder()
                                .sourceAccountId(ACCOUNT_1_ID)
                                .destinationAccountId(ACCOUNT_2_ID)
                                .amount(TRANSFER_AMOUNT)
                                .referenceNo(referenceNo)
                                .remarks("Stress transfer 1 -> 2")
                                .build();
                    } else {
                        request = TransferRequestDto.builder()
                                .sourceAccountId(ACCOUNT_2_ID)
                                .destinationAccountId(ACCOUNT_1_ID)
                                .amount(TRANSFER_AMOUNT)
                                .referenceNo(referenceNo)
                                .remarks("Stress transfer 2 -> 1")
                                .build();
                    }

                    TransferResponseDto response = accountBalanceService.executeTransfer(request, 1L, "127.0.0.1");
                    if ("COMPLETED".equals(response.getStatus())) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Step 3: Unleash all 50 threads simultaneously
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Step 4: Wait up to 60 seconds for all transfers to conclude
        boolean finishedInTime = doneLatch.await(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        System.out.printf("Stress test completed in %d ms:%n", elapsed);
        System.out.printf("  Successful transfers: %d / %d%n", successCount.get(), CONCURRENT_THREADS);
        System.out.printf("  Errors: %d%n", errors.size());

        if (!errors.isEmpty()) {
            errors.forEach(err -> System.err.printf("  Error occurred: %s%n", err.getMessage()));
        }

        // Step 5: Assertions
        assertThat(finishedInTime).as("All 50 threads must complete within timeout without deadlock hang").isTrue();
        assertThat(errors).as("No threads should encounter deadlocks or unhandled exceptions").isEmpty();
        assertThat(successCount.get()).as("All 50 transfers must successfully complete").isEqualTo(CONCURRENT_THREADS);

        // Step 6: Verify strict total balance invariant
        Balance finalBalance1 = balanceRepository.findReadOnlyByAccountId(ACCOUNT_1_ID).orElseThrow();
        Balance finalBalance2 = balanceRepository.findReadOnlyByAccountId(ACCOUNT_2_ID).orElseThrow();

        BigDecimal finalB1 = finalBalance1.getAvailableBalance();
        BigDecimal finalB2 = finalBalance2.getAvailableBalance();
        BigDecimal finalTotal = finalB1.add(finalB2);

        System.out.printf("Final Account 1: %s%n", finalB1);
        System.out.printf("Final Account 2: %s%n", finalB2);
        System.out.printf("Final Total: %s (Expected: %s)%n", finalTotal, expectedTotalInvariant);

        assertThat(finalTotal)
                .as("Sum of balances across Account 1 and Account 2 must strictly match the initial invariant")
                .isEqualByComparingTo(expectedTotalInvariant);

        // Since 25 transfers went 1 -> 2 and 25 transfers went 2 -> 1 for the exact same amount,
        // the individual balances must also return precisely to their initial values!
        assertThat(finalB1)
                .as("Account 1 balance should return to initial balance after balanced transfers")
                .isEqualByComparingTo(initialB1);
        assertThat(finalB2)
                .as("Account 2 balance should return to initial balance after balanced transfers")
                .isEqualByComparingTo(initialB2);
    }
}
