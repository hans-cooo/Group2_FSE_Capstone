package com.group2.fse.auth_service.security.blacklist;

import java.time.Duration;

public interface TokenBlacklistService {

    /**
     * Revokes a token by writing its jti to the Redis blacklist with the given TTL.
     */
    void revokeToken(String jti, Duration remainingTtl);

    /**
     * Revokes a token by parsing its jti and calculating the remaining TTL from the token.
     */
    void revokeTokenByJwt(String jwt);

    /**
     * Checks if a jti is in the Redis blacklist.
     */
    boolean isRevoked(String jti);
}
