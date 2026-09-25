# Tasks: FSE-401 Stateless Spring Security Configuration

## Phase 1: Environment & Dependency Setup
- [x] Task 1.1: Git feature branch setup (`feat/FSE-401-carl-security-config`)
- [x] Task 1.2: Add Spring Security dependencies (`spring-boot-starter-security`, `spring-security-test`) in `pom.xml`

## Checkpoint 1: Dependencies Ready
- [x] `./mvnw compile` runs successfully with Spring Security dependencies

## Phase 2: Decoupled Filter Stubs
- [x] Task 2.1: Implement optional filter injection / contract stubs for Jared (FSE-402), Gabriel (FSE-404), and Alyssa (FSE-405)

## Phase 3: Core Security Implementation
- [x] Task 3.1: Implement `CorsConfig.java` in `com.group2.fse.ledger_service.security.config`
- [x] Task 3.2: Implement `SecurityConfig.java` with stateless policy, CSRF disabled, endpoint route matching, and filter chain sequence

## Checkpoint 2: Core Security Compiles
- [x] Security configuration classes compile cleanly without errors

## Phase 4: Test Suite & Verification
- [x] Task 4.1: Write unit tests for `CorsConfigTest.java` and `SecurityConfigTest.java` verifying beans load, allowed headers, origins, methods, and credentials
- [x] Task 4.2: Verify regression safety on `IdempotencyInterceptorTest` and existing tests

## Checkpoint 3: Test Verification Complete
- [x] All security unit tests pass (CorsConfigTest: 2/2, SecurityConfigTest: 1/1)
- [x] Existing `IdempotencyInterceptorTest` passes 5/5 (8/8 tests pass overall)

## Phase 5: Documentation & Team Hand-off
- [ ] Task 5.1: Create PR for `feat/FSE-401-carl-security-config` into `dev` with integration documentation for teammates
