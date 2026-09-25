package com.group2.fse.ledger_service.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Task: FSE-301
 * Assigned to: Carl
 * Service contract for atomic idempotency mutex locks and cached response resolution.
 */
public interface IdempotencyService {

    /**
     * Attempts atomic acquisition of an idempotency lock via Redis SETNX.
     *
     * @param idempotencyKey the unique transaction idempotency key
     * @param lockTtl        the timeout for the lock to prevent permanent deadlock
     * @return true if acquired successfully, false if duplicate or in-flight
     */
    boolean tryAcquire(String idempotencyKey, Duration lockTtl);

    /**
     * Checks if the transaction is currently in-flight.
     */
    boolean isProcessing(String idempotencyKey);

    /**
     * Retrieves the cached HTTP response payload for an already-completed transaction.
     */
    Optional<String> getCachedResponse(String idempotencyKey);

    /**
     * Stores the final JSON response payload in cache and marks the key completed.
     */
    void complete(String idempotencyKey, String responseBodyJson, Duration cacheTtl);

    /**
     * Releases the lock in case of transaction failure, permitting client retry.
     */
    void release(String idempotencyKey);
}
