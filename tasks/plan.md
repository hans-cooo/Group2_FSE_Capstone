# Implementation Plan: FSE-401 Stateless Spring Security Configuration

## Overview
Implement **FSE-401: Stateless Spring Security Configuration** for the `ledger-service`. This establishes the perimeter security boundary for Sprint 2 (Epic D: Perimeter Token Security). The configuration enforces stateless session management (`SessionCreationPolicy.STATELESS`), disables CSRF for REST APIs, configures secure CORS policies, establishes endpoint access rules, and orchestrates the security filter sequence without causing merge conflicts with team members (Jared, Gabriel, Alyssa, Hans).

---

## Architecture Decisions & Strategy

1. **Zero-Overlap Decoupled Filter Architecture**:
   - Carl owns `com.group2.fse.ledger_service.security.config` (`SecurityConfig.java` and `CorsConfig.java`).
   - Collaborators implement independent `@Component` beans (`JwtAuthenticationFilter` by Jared, `TokenBlacklistFilter` by Gabriel, `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` by Alyssa).
   - In `SecurityConfig.java`, inject collaborator beans using optional or missing-bean fallbacks (`@Autowired(required = false)` or basic placeholder stubs) so Carl's branch compiles and runs 100% independently in CI/CD before teammates merge their branches.

2. **Canonical Filter Pipeline Ordering**:
   ```
   Client HTTP Request
          │
          ▼
   1. JwtAuthenticationFilter (FSE-402 - Jared)
          │  [Validates Bearer token signature, extracts claims]
          ▼
   2. TokenBlacklistFilter (FSE-404 - Gabriel)
          │  [Checks Redis blacklist by jti]
          ▼
   3. IdempotencyInterceptor (FSE-301 - Carl, already in dev)
          │  [Pre-flight Redis lock & duplicate check]
          ▼
   4. Controller & Method Security (FSE-403 - Hans)
             [@PreAuthorize role checks]
   ```
   Filter registration order:
   ```java
   http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
       .addFilterAfter(tokenBlacklistFilter, JwtAuthenticationFilter.class);
   ```

3. **Stateless Endpoint Authorization Rules**:
   - Permitted endpoints: `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error` $\rightarrow$ `permitAll()`
   - Protected banking API endpoints: `/api/v1/ledger/**` $\rightarrow$ `authenticated()`
   - All other endpoints: `authenticated()`

4. **Preserving Existing Test Integrity**:
   - Adding `spring-boot-starter-security` automatically activates default security for all endpoints in Spring Boot context tests.
   - We must provide `spring-security-test` with `@WithMockUser` annotations or test security exclusions so existing tests (like `IdempotencyInterceptorTest`) remain completely unaffected.

---

## Detailed Task Breakdown

### Phase 1: Environment & Dependency Setup
- [ ] **Task 1.1: Git Branch Setup**
  - Pull latest `origin/dev`.
  - Create feature branch `feat/FSE-401-carl-security-config`.
- [ ] **Task 1.2: Maven Dependencies Update**
  - Add `spring-boot-starter-security` to `backend/ledger-service/pom.xml`.
  - Add `spring-security-test` with `test` scope.
  - Verify Maven compilation with `./mvnw compile`.

### Checkpoint: Dependencies & Build Ready
- [ ] Application compiles cleanly with Spring Security dependencies present.

---

### Phase 2: Decoupling Stubs & Contract Interfaces
- [ ] **Task 2.1: Collaborator Component Stubs / Declarations**
  - Create package `com.group2.fse.ledger_service.security.config`.
  - Implement contract stubs or fallback beans for:
    - `JwtAuthenticationFilter` (FSE-402 stub)
    - `TokenBlacklistFilter` (FSE-404 stub)
    - `CustomAuthenticationEntryPoint` & `CustomAccessDeniedHandler` (FSE-405 stubs)
  - Ensure real beans will cleanly override these stubs once teammates merge.

---

### Phase 3: Core Security & CORS Implementation
- [ ] **Task 3.1: Implement `CorsConfig.java`**
  - Define `CorsConfigurationSource` bean.
  - Configure allowed origins (from `app.cors.allowed-origins` property or banking defaults), methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`), headers (`Authorization`, `Idempotency-Key`, `Content-Type`), and cache max-age (3600s).
- [ ] **Task 3.2: Implement `SecurityConfig.java`**
  - Annotate with `@Configuration` and `@EnableWebSecurity`.
  - Configure `SecurityFilterChain`:
    - `csrf(AbstractHttpConfigurer::disable)`
    - `sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`
    - `cors(c -> c.configurationSource(corsConfigurationSource()))`
    - `authorizeHttpRequests(...)`
    - Exception handling delegating to custom entry point and access denied handler.
    - Filter registration order wiring.

### Checkpoint: Security Configuration Compiles
- [ ] `SecurityConfig` and `CorsConfig` build with `./mvnw compile`.

---

### Phase 4: Test Suite & Regression Verification
- [ ] **Task 4.1: Unit & Security Slice Tests**
  - Create `SecurityConfigTest.java` using `@SpringBootTest` or `@WebMvcTest`.
  - Verify unauthenticated requests to `/api/v1/ledger/test` return `401 Unauthorized`.
  - Verify requests to `/actuator/health` return `200 OK` without credentials.
  - Verify CORS pre-flight `OPTIONS` requests receive appropriate CORS headers.
- [ ] **Task 4.2: Existing Test Suite Regression**
  - Run `IdempotencyInterceptorTest` to confirm 5/5 tests continue to pass.
  - Run full test suite `./mvnw test` to ensure zero regressions across all modules.

---

### Phase 5: Documentation & PR Readiness
- [ ] **Task 5.1: PR & Team Integration Hand-off**
  - Verify clean git diff.
  - Document bean extension points for Jared, Gabriel, and Alyssa in PR description.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
| :--- | :---: | :--- |
| **Spring Security breaks existing MockMvc tests** | High | Add `spring-security-test` and use `@WithMockUser` or test security bypass configurations for existing unit tests. |
| **Teammate filters aren't ready when wiring filter chain** | Medium | Use conditional injection (`@Autowired(required = false)`) so `SecurityConfig` operates gracefully whether filters are present or pending. |
| **Filter sequence misconfiguration** | High | Strictly follow the documented sequence (`JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`, `TokenBlacklistFilter` after `JwtAuthenticationFilter`). |
| **CORS blocking pre-flight requests** | Medium | Explicitly permit `HttpMethod.OPTIONS` and integrate `CorsConfigurationSource` directly into `http.cors()`. |
