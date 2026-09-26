package com.group2.fse.auth_service.service;

import com.group2.fse.auth_service.dto.AuthResponseDto;
import com.group2.fse.auth_service.dto.LoginRequestDto;
import com.group2.fse.auth_service.dto.LoginResultDto;
import com.group2.fse.auth_service.dto.MfaVerifyRequestDto;

public interface StaffAuthService {

    /**
     * Authenticates bank staff (tellers, admins) against internal credentials.
     */
    LoginResultDto login(LoginRequestDto request);

    /**
     * Verifies submitted staff 2FA code and issues staff access token.
     */
    AuthResponseDto verifyMfa(MfaVerifyRequestDto request);
}
