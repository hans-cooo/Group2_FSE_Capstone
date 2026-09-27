package com.group2.fse.account_service.security.blacklist;

public interface TokenBlacklistService {
    boolean isBlacklisted(String jti);
    void blacklistToken(String jti, long ttlMillis);
}
