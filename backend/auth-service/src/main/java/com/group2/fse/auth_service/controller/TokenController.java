package com.group2.fse.auth_service.controller;

import com.group2.fse.auth_service.dto.AuthResponseDto;
import com.group2.fse.auth_service.dto.RefreshTokenRequestDto;
import com.group2.fse.auth_service.dto.UserProfileDto;
import com.group2.fse.auth_service.exception.InvalidTokenException;
import com.group2.fse.auth_service.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for token lifecycle operations (refresh, revoke, me).
 */
@RestController
@RequestMapping("/api/v1/auth/token")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("REST request to refresh access token");
        AuthResponseDto response = tokenService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("REST request to revoke token session");
        if (authorization == null || authorization.isBlank()) {
            throw new InvalidTokenException("Missing Authorization header");
        }
        tokenService.revokeToken(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("REST request to get current user profile");
        if (authorization == null || authorization.isBlank()) {
            throw new InvalidTokenException("Missing Authorization header");
        }
        UserProfileDto profile = tokenService.getCurrentUserProfile(authorization);
        return ResponseEntity.ok(profile);
    }
}
