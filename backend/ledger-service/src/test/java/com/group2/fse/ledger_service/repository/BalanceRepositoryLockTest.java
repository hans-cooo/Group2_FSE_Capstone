package com.group2.fse.ledger_service.repository;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.group2.fse.ledger_service.entity.Balance;

@SpringBootTest
class BalanceRepositoryLockTest {

    @Autowired
    private BalanceRepository balanceRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldLockBalanceRowUntilFirstTransactionCompletes() throws Exception {

        Long accountId = 1L; // existing account_id in BALANCE table

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch firstLockAcquired = new CountDownLatch(1);

        Future<Long> thread1 = executor.submit(() -> {

            TransactionStatus tx =
                    transactionManager.getTransaction(
                            new DefaultTransactionDefinition(
                                    TransactionDefinition.PROPAGATION_REQUIRED));

            try {

                Optional<Balance> balance =
                        balanceRepository.findByAccountId(accountId);

                if (balance.isEmpty()) {
                    throw new RuntimeException("Balance not found");
                }

                firstLockAcquired.countDown();

                Thread.sleep(5000);

                transactionManager.commit(tx);

                return System.currentTimeMillis();

            } catch (Exception e) {
                firstLockAcquired.countDown();
                transactionManager.rollback(tx);
                throw e;
            }
        });

        assertTrue(
                firstLockAcquired.await(10, java.util.concurrent.TimeUnit.SECONDS),
                "Thread 1 timed out acquiring initial lock"
        );

        Future<Long> thread2 = executor.submit(() -> {

            long start = System.currentTimeMillis();

            TransactionStatus tx =
                    transactionManager.getTransaction(
                            new DefaultTransactionDefinition(
                                    TransactionDefinition.PROPAGATION_REQUIRED));

            try {

                balanceRepository.findByAccountId(accountId);

                transactionManager.commit(tx);

                return System.currentTimeMillis() - start;

            } catch (Exception e) {

                transactionManager.rollback(tx);
                throw e;
            }
        });

        thread1.get();
        long waitTime = thread2.get();

        executor.shutdown();

        assertTrue(
                waitTime >= 4000,
                "Second transaction did not wait for lock release"
        );
    }
}
