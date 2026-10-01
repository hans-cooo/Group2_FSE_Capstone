package com.group2.fse.auth_service.controller;

import com.group2.fse.auth_service.dto.*;
import com.group2.fse.auth_service.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for customer identity, registration, and authentication flows.
 */
@RestController
@RequestMapping("/api/v1/auth/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody CustomerRegistrationDto request) {
        log.info("REST request to register customer username: {}", request.getUsername());
        AuthResponseDto response = customerAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResultDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("REST request for customer login: {}", request.getUsername());
        LoginResultDto result = customerAuthService.login(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponseDto> verifyMfa(@Valid @RequestBody MfaVerifyRequestDto request) {
        log.info("REST request for customer MFA verification");
        AuthResponseDto response = customerAuthService.verifyMfa(request);
        return ResponseEntity.ok(response);
    }
}
