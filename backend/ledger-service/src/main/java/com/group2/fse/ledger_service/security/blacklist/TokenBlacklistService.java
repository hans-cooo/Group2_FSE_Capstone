package com.group2.fse.ledger_service.security.blacklist;

import java.time.Duration;

/**
 * Task: FSE-404
 * Assigned to: Gabriel
 * Contract for revoking JWTs by their jti (JWT ID) claim and checking
 * revocation status. Backed by Redis, keyspace blacklist:jti:{jti}.
 */
public interface TokenBlacklistService {
    void revokeToken(String jti, Duration remainingTtl);
    boolean isRevoked(String jti);
}