package com.group2.fse.auth_service.service.impl;

import com.group2.fse.auth_service.dto.AuthResponseDto;
import com.group2.fse.auth_service.dto.LoginRequestDto;
import com.group2.fse.auth_service.dto.LoginResultDto;
import com.group2.fse.auth_service.dto.MfaVerifyRequestDto;
import com.group2.fse.auth_service.entity.User;
import com.group2.fse.auth_service.exception.AccountInactiveException;
import com.group2.fse.auth_service.exception.InvalidCredentialsException;
import com.group2.fse.auth_service.exception.MfaVerificationException;
import com.group2.fse.auth_service.repository.UserRepository;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import com.group2.fse.auth_service.security.mfa.MfaChallenge;
import com.group2.fse.auth_service.security.mfa.MfaChallengeService;
import com.group2.fse.auth_service.security.session.RedisRefreshTokenService;
import com.group2.fse.auth_service.service.StaffAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAuthServiceImpl implements StaffAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenService refreshTokenService;
    private final MfaChallengeService mfaChallengeService;

    @Value("${app.security.mfa.enabled:false}")
    private boolean mfaEnabled;

    @Override
    @Transactional(readOnly = true)
    public LoginResultDto login(LoginRequestDto request) {
        log.info("Bank staff login attempt for username: {}", request.getUsername());

        User staff = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPasswordHash())) {
            log.warn("Invalid password for staff username: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
            log.warn("Staff account is not active: status={}", staff.getStatus());
            throw new AccountInactiveException("Staff account is suspended or inactive");
        }

        List<String> roles = resolveStaffRoles(staff.getRole().getRoleName());

        if (mfaEnabled) {
            log.info("MFA challenge triggered for staff: {}", staff.getUsername());
            String channel = resolveChannel(request.getPreferredChannel(), "EMAIL");
            String destination;
            String maskedDestination;

            if ("TOTP".equalsIgnoreCase(channel)) {
                destination = "Authenticator App (Google/Microsoft Authenticator)";
                maskedDestination = "Authenticator App (RFC 6238)";
            } else if ("SMS".equalsIgnoreCase(channel)) {
                destination = "+63 917 000 0000"; // Or staff phone if stored
                maskedDestination = "+63 917 **** 000";
            } else {
                channel = "EMAIL";
                destination = staff.getEmail();
                maskedDestination = maskEmail(staff.getEmail());
            }

            MfaChallenge challenge = mfaChallengeService.createChallenge(
                    staff.getUserId(), staff.getUsername(), roles, "STAFF", channel, destination);

            return LoginResultDto.builder()
                    .mfaRequired(true)
                    .mfaToken(challenge.getMfaToken())
                    .deliveryChannel(channel)
                    .maskedDestination(maskedDestination)
                    .build();
        }

        AuthResponseDto authData = issueTokenPair(staff.getUserId(), staff.getUsername(), roles, "STAFF");
        return LoginResultDto.builder()
                .mfaRequired(false)
                .authData(authData)
                .build();
    }

    @Override
    public AuthResponseDto verifyMfa(MfaVerifyRequestDto request) {
        MfaChallenge challenge = mfaChallengeService.verifyChallenge(request.getMfaToken(), request.getCode())
                .orElseThrow(() -> new MfaVerificationException("Invalid or expired staff 2FA code"));

        if (!"STAFF".equals(challenge.getUserType())) {
            throw new MfaVerificationException("Invalid user type for staff MFA verification");
        }

        log.info("MFA verification successful for staff: {}", challenge.getUsername());
        return issueTokenPair(challenge.getUserId(), challenge.getUsername(), challenge.getRoles(), "STAFF");
    }

    private List<String> resolveStaffRoles(String primaryRole) {
        List<String> roles = new ArrayList<>();
        roles.add(primaryRole);
        // Admin inherits teller operations
        if ("ROLE_ADMIN".equals(primaryRole) && !roles.contains("ROLE_TELLER")) {
            roles.add("ROLE_TELLER");
        }
        return roles;
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

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@corebank.local";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        String visible = name.length() > 2 ? name.substring(0, 2) : name.substring(0, 1);
        return visible + "****@" + domain;
    }

    private String resolveChannel(String preferred, String defaultChannel) {
        if (preferred == null || preferred.isBlank()) {
            return defaultChannel;
        }
        String p = preferred.trim().toUpperCase();
        if ("TOTP".equals(p) || "EMAIL".equals(p) || "SMS".equals(p)) {
            return p;
        }
        return defaultChannel;
    }
}
