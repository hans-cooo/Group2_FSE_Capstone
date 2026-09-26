package com.group2.fse.auth_service.service;

import com.group2.fse.auth_service.dto.*;
import com.group2.fse.auth_service.exception.InvalidCredentialsException;
import com.group2.fse.auth_service.exception.UserAlreadyExistsException;
import com.group2.fse.auth_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import com.group2.fse.auth_service.repository.CustomerRepository;
import com.group2.fse.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Phase 4: Auth Business Logic Services Integration Tests")
class AuthBusinessServiceIntegrationTest {

    @Autowired
    private CustomerAuthService customerAuthService;

    @Autowired
    private StaffAuthService staffAuthService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        String encoded = passwordEncoder.encode("Password123!");
        userRepository.findByUsername("admin").ifPresent(u -> {
            u.setPasswordHash(encoded);
            userRepository.save(u);
        });
        userRepository.findByUsername("teller_alice").ifPresent(u -> {
            u.setPasswordHash(encoded);
            userRepository.save(u);
        });
        customerRepository.findByUsername("john_doe").ifPresent(c -> {
            c.setPasswordHash(encoded);
            customerRepository.save(c);
        });
    }

    @Test
    @DisplayName("Should register new customer and immediately issue valid JWT pair")
    void shouldRegisterNewCustomer() {
        String uniqueUser = "client_" + System.currentTimeMillis();
        CustomerRegistrationDto registrationDto = CustomerRegistrationDto.builder()
                .username(uniqueUser)
                .email(uniqueUser + "@bank.ph")
                .password("SecurePass123!")
                .firstName("Juan")
                .middleInitial("M")
                .lastName("Dela Cruz")
                .address("77 Ayala Avenue, Makati")
                .mobileNumber("+639171112233")
                .civilStatus("SINGLE")
                .occupation("Developer")
                .build();

        AuthResponseDto response = customerAuthService.register(registrationDto);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo(uniqueUser);
        assertThat(response.getRoles()).containsExactly("ROLE_CUSTOMER");
        assertThat(response.getUserType()).isEqualTo("CUSTOMER");

        // Validate that token works with jwtTokenProvider
        assertThat(jwtTokenProvider.validateToken(response.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.getUsername(response.getAccessToken())).isEqualTo(uniqueUser);
    }

    @Test
    @DisplayName("Should prevent registration with duplicate username or email")
    void shouldRejectDuplicateRegistration() {
        CustomerRegistrationDto dupUser = CustomerRegistrationDto.builder()
                .username("john_doe") // Already in seed data
                .email("new_unique_email@bank.ph")
                .password("SecurePass123!")
                .firstName("John")
                .lastName("Doe")
                .address("123 Street")
                .mobileNumber("+639170000000")
                .build();

        assertThatThrownBy(() -> customerAuthService.register(dupUser))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("john_doe");

        CustomerRegistrationDto dupEmail = CustomerRegistrationDto.builder()
                .username("completely_unique_name")
                .email("john.doe@example.com") // Already in seed data
                .password("SecurePass123!")
                .firstName("John")
                .lastName("Doe")
                .address("123 Street")
                .mobileNumber("+639170000000")
                .build();

        assertThatThrownBy(() -> customerAuthService.register(dupEmail))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("john.doe@example.com");
    }

    @Test
    @DisplayName("Should authenticate seed customer with correct password")
    void shouldAuthenticateSeedCustomer() {
        LoginRequestDto loginDto = LoginRequestDto.builder()
                .username("john_doe")
                .password("Password123!")
                .build();

        LoginResultDto result = customerAuthService.login(loginDto);

        assertThat(result.isMfaRequired()).isFalse();
        assertThat(result.getAuthData()).isNotNull();
        assertThat(result.getAuthData().getUsername()).isEqualTo("john_doe");
        assertThat(result.getAuthData().getRoles()).containsExactly("ROLE_CUSTOMER");
        assertThat(jwtTokenProvider.validateToken(result.getAuthData().getAccessToken())).isTrue();
    }

    @Test
    @DisplayName("Should reject customer login with incorrect password")
    void shouldRejectCustomerWithInvalidPassword() {
        LoginRequestDto badPass = LoginRequestDto.builder()
                .username("john_doe")
                .password("WrongPassword999!")
                .build();

        assertThatThrownBy(() -> customerAuthService.login(badPass))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Should authenticate seed admin and inherit teller role")
    void shouldAuthenticateAdminWithInheritedRoles() {
        LoginRequestDto adminLogin = LoginRequestDto.builder()
                .username("admin")
                .password("Password123!")
                .build();

        LoginResultDto result = staffAuthService.login(adminLogin);

        assertThat(result.isMfaRequired()).isFalse();
        assertThat(result.getAuthData()).isNotNull();
        assertThat(result.getAuthData().getUsername()).isEqualTo("admin");
        assertThat(result.getAuthData().getRoles()).contains("ROLE_ADMIN", "ROLE_TELLER");
        assertThat(result.getAuthData().getUserType()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("Should authenticate seed teller staff")
    void shouldAuthenticateTellerStaff() {
        LoginRequestDto tellerLogin = LoginRequestDto.builder()
                .username("teller_alice")
                .password("Password123!")
                .build();

        LoginResultDto result = staffAuthService.login(tellerLogin);

        assertThat(result.isMfaRequired()).isFalse();
        assertThat(result.getAuthData()).isNotNull();
        assertThat(result.getAuthData().getUsername()).isEqualTo("teller_alice");
        assertThat(result.getAuthData().getRoles()).containsExactly("ROLE_TELLER");
    }

    @Test
    @DisplayName("Should rotate refresh token and issue fresh access token")
    void shouldRotateRefreshToken() {
        LoginResultDto loginResult = customerAuthService.login(LoginRequestDto.builder()
                .username("john_doe")
                .password("Password123!")
                .build());

        String initialRefreshToken = loginResult.getAuthData().getRefreshToken();

        AuthResponseDto refreshed = tokenService.refreshAccessToken(initialRefreshToken);

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(initialRefreshToken);
        assertThat(refreshed.getUsername()).isEqualTo("john_doe");
        assertThat(jwtTokenProvider.validateToken(refreshed.getAccessToken())).isTrue();
    }

    @Test
    @DisplayName("Should revoke token session and write jti to blacklist")
    void shouldRevokeTokenSession() {
        LoginResultDto loginResult = customerAuthService.login(LoginRequestDto.builder()
                .username("john_doe")
                .password("Password123!")
                .build());

        String accessToken = loginResult.getAuthData().getAccessToken();
        String jti = jwtTokenProvider.getJti(accessToken);

        assertThat(tokenBlacklistService.isRevoked(jti)).isFalse();

        tokenService.revokeToken("Bearer " + accessToken);

        assertThat(tokenBlacklistService.isRevoked(jti)).isTrue();
    }

    @Test
    @DisplayName("Should retrieve full user profile from Bearer token")
    void shouldGetUserProfileFromToken() {
        LoginResultDto loginResult = customerAuthService.login(LoginRequestDto.builder()
                .username("john_doe")
                .password("Password123!")
                .build());

        String accessToken = loginResult.getAuthData().getAccessToken();

        UserProfileDto profile = tokenService.getCurrentUserProfile("Bearer " + accessToken);

        assertThat(profile.getUsername()).isEqualTo("john_doe");
        assertThat(profile.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(profile.getUserType()).isEqualTo("CUSTOMER");
        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
    }
}
