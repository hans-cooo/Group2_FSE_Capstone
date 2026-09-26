package com.group2.fse.auth_service.service;

import com.group2.fse.auth_service.dto.AuthResponseDto;
import com.group2.fse.auth_service.dto.UserProfileDto;

public interface TokenService {

    /**
     * Exchanges an active 7-day refresh token for a newly signed access token, rotating the refresh token.
     */
    AuthResponseDto refreshAccessToken(String refreshToken);

    /**
     * Revokes the current session by writing the token's jti to the Redis blacklist.
     */
    void revokeToken(String bearerToken);

    /**
     * Retrieves the profile information for the authenticated user.
     */
    UserProfileDto getCurrentUserProfile(String bearerToken);
}
