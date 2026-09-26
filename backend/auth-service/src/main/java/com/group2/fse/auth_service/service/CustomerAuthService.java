package com.group2.fse.auth_service.service;

import com.group2.fse.auth_service.dto.AuthResponseDto;
import com.group2.fse.auth_service.dto.CustomerRegistrationDto;
import com.group2.fse.auth_service.dto.LoginRequestDto;
import com.group2.fse.auth_service.dto.LoginResultDto;
import com.group2.fse.auth_service.dto.MfaVerifyRequestDto;

public interface CustomerAuthService {

    /**
     * Registers a new retail customer, creates their initial KYC profile, and returns an auth token pair.
     */
    AuthResponseDto register(CustomerRegistrationDto request);

    /**
     * Authenticates retail customer credentials and returns either tokens or an MFA challenge.
     */
    LoginResultDto login(LoginRequestDto request);

    /**
     * Verifies submitted OTP against customer MFA challenge and issues token pair.
     */
    AuthResponseDto verifyMfa(MfaVerifyRequestDto request);
}
