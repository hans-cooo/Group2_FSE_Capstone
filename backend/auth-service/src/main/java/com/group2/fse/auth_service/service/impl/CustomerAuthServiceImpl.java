package com.group2.fse.auth_service.service.impl;

import com.group2.fse.auth_service.dto.*;
import com.group2.fse.auth_service.entity.Customer;
import com.group2.fse.auth_service.entity.Kyc;
import com.group2.fse.auth_service.exception.InvalidCredentialsException;
import com.group2.fse.auth_service.exception.MfaVerificationException;
import com.group2.fse.auth_service.exception.UserAlreadyExistsException;
import com.group2.fse.auth_service.repository.CustomerRepository;
import com.group2.fse.auth_service.repository.KycRepository;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import com.group2.fse.auth_service.security.mfa.MfaChallenge;
import com.group2.fse.auth_service.security.mfa.MfaChallengeService;
import com.group2.fse.auth_service.security.session.RedisRefreshTokenService;
import com.group2.fse.auth_service.service.CustomerAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final KycRepository kycRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenService refreshTokenService;
    private final MfaChallengeService mfaChallengeService;

    @Value("${app.security.mfa.enabled:false}")
    private boolean mfaEnabled;

    @Override
    @Transactional
    public AuthResponseDto register(CustomerRegistrationDto request) {
        log.info("Attempting customer registration for username: {}", request.getUsername());

        if (customerRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already registered");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Customer customer = Customer.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .kycStatus("VERIFIED")
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        Kyc kyc = Kyc.builder()
                .customer(savedCustomer)
                .firstName(request.getFirstName())
                .middleInitial(request.getMiddleInitial())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .civilStatus(request.getCivilStatus())
                .occupation(request.getOccupation())
                .mobileNumber(request.getMobileNumber())
                .status("VERIFIED")
                .build();

        kycRepository.save(kyc);
        log.info("Successfully registered customer: id={}, username={}", savedCustomer.getCustomerId(), savedCustomer.getUsername());

        return issueTokenPair(savedCustomer.getCustomerId(), savedCustomer.getUsername(), List.of("ROLE_CUSTOMER"), "CUSTOMER");
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResultDto login(LoginRequestDto request) {
        log.info("Customer login attempt for username: {}", request.getUsername());

        Customer customer = customerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            log.warn("Invalid password for customer username: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        List<String> roles = List.of("ROLE_CUSTOMER");

        if (mfaEnabled) {
            log.info("MFA challenge triggered for customer: {}", customer.getUsername());
            MfaChallenge challenge = mfaChallengeService.createChallenge(
                    customer.getCustomerId(), customer.getUsername(), roles, "CUSTOMER");

            String maskedPhone = maskDestination(customer.getCustomerId());

            return LoginResultDto.builder()
                    .mfaRequired(true)
                    .mfaToken(challenge.getMfaToken())
                    .deliveryChannel("SMS")
                    .maskedDestination(maskedPhone)
                    .build();
        }

        AuthResponseDto authData = issueTokenPair(customer.getCustomerId(), customer.getUsername(), roles, "CUSTOMER");
        return LoginResultDto.builder()
                .mfaRequired(false)
                .authData(authData)
                .build();
    }

    @Override
    public AuthResponseDto verifyMfa(MfaVerifyRequestDto request) {
        MfaChallenge challenge = mfaChallengeService.verifyChallenge(request.getMfaToken(), request.getCode())
                .orElseThrow(() -> new MfaVerificationException("Invalid or expired 2FA verification code"));

        if (!"CUSTOMER".equals(challenge.getUserType())) {
            throw new MfaVerificationException("Invalid user type for customer MFA verification");
        }

        log.info("MFA verification successful for customer: {}", challenge.getUsername());
        return issueTokenPair(challenge.getUserId(), challenge.getUsername(), challenge.getRoles(), "CUSTOMER");
    }

    private AuthResponseDto issueTokenPair(Long userId, String username, List<String> roles, String userType) {
        String accessToken = jwtTokenProvider.generateAccessToken(username, userId, roles, userType);
        String refreshToken = refreshTokenService.createRefreshToken(userId, username, roles, userType);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs() / 1000)
                .userId(userId)
                .username(username)
                .roles(roles)
                .userType(userType)
                .build();
    }

    private String maskDestination(Long customerId) {
        return kycRepository.findByCustomerCustomerId(customerId)
                .map(kyc -> {
                    String num = kyc.getMobileNumber();
                    if (num != null && num.length() >= 7) {
                        return num.substring(0, 4) + " **** " + num.substring(num.length() - 3);
                    }
                    return "+63 9** **** ***";
                })
                .orElse("+63 9** **** ***");
    }
}
