# Ledger Service Implementation Task List

## Phase 1: Security Perimeter & JWT Authentication Pipeline

### Task 1.1: Security & JJWT Maven Dependencies Setup
**Description:** Add Spring Security, JJWT (API, Impl, Jackson 0.12.6), and Spring Security Test to `pom.xml`.
**Acceptance criteria:**
- [x] `spring-boot-starter-security` and `spring-security-test` added to `backend/ledger-service/pom.xml`.
- [x] JJWT dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` version 0.12.6) added.
- [x] Maven build passes with `./mvnw clean compile`.
**Verification:**
- [x] Build succeeds: `./mvnw compile` in `backend/ledger-service`
**Dependencies:** None
**Files likely touched:**
- `backend/ledger-service/pom.xml`
**Estimated scope:** Small (1 file)

---

### Task 1.2: JWT Utilities & Claims Parser
**Description:** Implement `JwtTokenProvider` to parse, validate signatures, and extract claims (`username`, `userId`, `roles`, `jti`) from RSA256 / HMAC JWT tokens.
**Acceptance criteria:**
- [x] Parses standard Bearer tokens and extracts subject (`username`/`userId`), `roles` list, and `jti` (JWT ID).
- [x] Validates expiration and signature integrity.
- [x] Unit tests verify valid token parsing and rejection of expired/tampered tokens.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=JwtTokenProviderTest`
**Dependencies:** Task 1.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/jwt/JwtTokenProvider.java`
- `backend/ledger-service/src/main/resources/application.properties`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/security/jwt/JwtTokenProviderTest.java`
**Estimated scope:** Medium (3 files)

---

### Task 1.3: Stateless JWT Authentication Filter
**Description:** Implement `JwtAuthenticationFilter` (`OncePerRequestFilter`) that extracts Bearer token, validates it via `JwtTokenProvider`, and populates `SecurityContextHolder`.
**Acceptance criteria:**
- [x] Inspects `Authorization: Bearer <token>` header.
- [x] Ignores public endpoints (`/actuator/health`, Swagger docs).
- [x] Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` with parsed user authorities.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=JwtAuthenticationFilterTest`
**Dependencies:** Task 1.2
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/filter/JwtAuthenticationFilter.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/security/filter/JwtAuthenticationFilterTest.java`
**Estimated scope:** Small (2 files)

---

### Task 1.4: Distributed Token Blacklist Integration
**Description:** Integrate existing `TokenBlacklistFilter` and `RedisTokenBlacklistServiceImpl` into the security filter chain to reject revoked tokens by `jti`.
**Acceptance criteria:**
- [x] `TokenBlacklistFilter` runs immediately after `JwtAuthenticationFilter`.
- [x] Extracts `jti` from parsed token or header and checks Redis blacklist.
- [x] Returns RFC-7807 401 Unauthorized (`AUTH_TOKEN_REVOKED`) if token is blacklisted.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=RedisTokenBlacklistServiceImplTest,TokenBlacklistFilterTest`
**Dependencies:** Task 1.3
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/blacklist/TokenBlacklistFilter.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/config/SecurityConfig.java`
**Estimated scope:** Small (2 files)

---

### Task 1.5: RFC-7807 Security Exception Handlers
**Description:** Implement `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` returning RFC-7807 `application/problem+json` envelopes.
**Acceptance criteria:**
- [x] Unauthenticated requests produce HTTP 401 with `AUTH_INVALID_CREDENTIALS` / `AUTH_TOKEN_EXPIRED` problem JSON.
- [x] Insufficient authority requests produce HTTP 403 with `AUTH_ACCESS_DENIED` problem JSON.
- [x] Response headers include `Content-Type: application/problem+json`.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=SecurityExceptionHandlerTest`
**Dependencies:** Task 1.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/handler/CustomAuthenticationEntryPoint.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/handler/CustomAccessDeniedHandler.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/security/handler/SecurityExceptionHandlerTest.java`
**Estimated scope:** Medium (3 files)

---

### Task 1.6: SecurityFilterChain & CORS Configuration
**Description:** Complete `SecurityConfig.java` and `CorsConfig.java` establishing stateless session management, CSRF disablement, CORS allowlist, and endpoint authorization rules.
**Acceptance criteria:**
- [x] `SecurityFilterChain` enforces `SessionCreationPolicy.STATELESS` and disables CSRF.
- [x] Permit all on `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`.
- [x] Authenticate all requests to `/api/v1/ledger/**`.
- [x] Registers `JwtAuthenticationFilter` and `TokenBlacklistFilter` in correct sequence.
- [x] Configures `CorsConfig` allowing `Authorization`, `Idempotency-Key`, `Content-Type` headers.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=SecurityConfigTest,CorsConfigTest`
**Dependencies:** Tasks 1.3, 1.4, 1.5
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/config/SecurityConfig.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/security/config/CorsConfig.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/security/SecurityConfigTest.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/security/CorsConfigTest.java`
**Estimated scope:** Medium (4 files)

---

### Checkpoint: Security Perimeter Verified
- [x] Public routes accessible without token.
- [x] Protected ledger routes reject unauthenticated requests with 401 problem details.
- [x] Valid token authenticates successfully.
- [x] All security tests pass cleanly: `./mvnw test -Dtest=*Security*,*Token*,*Cors*`

---

## Phase 2: Double-Entry Atomic Transfer & Concurrency Defense

### Task 2.1: Deadlock-Free Ordered Pessimistic Locking Queries
**Description:** Enhance `BalanceRepository` with ordered lock queries and Oracle query lock timeout hints (`jakarta.persistence.lock.timeout = 5000`).
**Acceptance criteria:**
- [x] Query hints configured for pessimistic write lock timeout (5000ms).
- [x] Ordered lock retrieval method added to support multi-account locking.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=BalanceRepositoryLockTest`
**Dependencies:** None
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/repository/BalanceRepository.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/repository/BalanceRepositoryLockTest.java`
**Estimated scope:** Small (2 files)

---

### Task 2.2: DTOs & Validation Contracts
**Description:** Implement immutable request and response DTOs for transfers, debits, credits, and balance queries.
**Acceptance criteria:**
- [x] `TransferRequestDto`: validates positive `amount`, non-null `sourceAccountId`, `destinationAccountId`, `referenceNo`. Disallows same source & destination ID.
- [x] `DebitCreditRequestDto`: validates positive `amount`, `accountId`, `referenceNo`.
- [x] `TransferResponseDto`, `DebitCreditResponseDto`, `BalanceResponseDto`: full fields matching `API_DESIGN_SPECIFICATION.md`.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=DtoValidationTest`
**Dependencies:** None
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/dto/TransferRequestDto.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/dto/TransferResponseDto.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/dto/DebitCreditRequestDto.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/dto/DebitCreditResponseDto.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/dto/BalanceResponseDto.java`
**Estimated scope:** Medium (5 files)

---

### Task 2.3: Double-Entry Atomic Transfer Service Implementation
**Description:** Implement `executeTransfer` in `AccountBalanceServiceImpl` enforcing deadlock-free lock ordering `[min(src, dst), max(src, dst)]`, non-negative balance invariant ($B_{\text{rem}} \ge 0$), paired `TRANSACTION` records, and PostgreSQL dual-write auditing.
**Acceptance criteria:**
- [x] Locks accounts in ascending numeric order: `min(src, dst)` followed by `max(src, dst)`.
- [x] Throws `InsufficientFundsException` if source balance $< \text{amount}$.
- [x] Creates two paired `Transaction` entities (DEBIT on source, CREDIT on destination) sharing `referenceNo`.
- [x] Dual-writes audit logs to PostgreSQL for both debited and credited accounts.
- [x] Rolls back entire transaction if PostgreSQL audit write fails.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=AccountBalanceServiceTransferTest,AccountBalanceServiceTest`
**Dependencies:** Tasks 2.1, 2.2
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/service/AccountBalanceService.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/service/impl/AccountBalanceServiceImpl.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/service/AccountBalanceServiceTransferTest.java`
**Estimated scope:** Medium (3 files)

---

### Task 2.4: Dual-Write Audit Cleanup & Consolidation
**Description:** Deprecate or retire duplicate `PostgresLedgerAuditWriter` and uncalled `CompensationManager`, centralizing all audit logging on `DualWriteLedgerAuditService`.
**Acceptance criteria:**
- [x] Remove or deprecate unused `PostgresLedgerAuditWriter` to eliminate dead code.
- [x] Remove uncalled `CompensationManager` (Spring `@Transactional` provides synchronous ACID rollback).
- [x] `DualWriteLedgerAuditService` serves as the authoritative synchronous audit bridge.
**Verification:**
- [x] Build succeeds: `./mvnw test-compile`
**Dependencies:** Task 2.3
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/audit/impl/PostgresLedgerAuditWriter.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/audit/CompensationManager.java`
**Estimated scope:** Small (2 files)

---

### Checkpoint: Atomic Transfer & Concurrency Verified
- [x] Atomic transfers function correctly with zero balance leakage.
- [x] Zero-overdraft invariant strictly enforced ($B_{\text{rem}} \ge 0$).
- [x] PostgreSQL audit records synchronously created on every mutation.
- [x] All service and repository tests pass cleanly.

---

## Phase 3: Production REST Controllers & RFC-7807 Global Exception Advice

### Task 3.1: Global Exception Handler Advice
**Description:** Implement `GlobalExceptionHandler` (`@RestControllerAdvice`) translating domain exceptions to RFC-7807 `application/problem+json` error envelopes.
**Acceptance criteria:**
- [x] Maps `InsufficientFundsException` to HTTP 422 (`INSUFFICIENT_FUNDS`).
- [x] Maps `AccountNotFoundException` to HTTP 404 (`ACCOUNT_NOT_FOUND`).
- [x] Maps `DuplicateIdempotencyKeyException` to HTTP 409 (`DUPLICATE_IDEMPOTENCY_KEY`).
- [x] Maps `MethodArgumentNotValidException` to HTTP 400 (`INVALID_REQUEST_PARAMETERS`).
- [x] Maps `OptimisticLockingFailureException` / lock timeouts to HTTP 409 (`CONCURRENT_TRANSACTION_IN_FLIGHT`).
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=GlobalExceptionHandlerTest`
**Dependencies:** Task 2.2
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/exception/GlobalExceptionHandler.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/exception/GlobalExceptionHandlerTest.java`
**Estimated scope:** Small (2 files)

---

### Task 3.2: Authoritative Balance Mutations Controller
**Description:** Implement `LedgerMutationController` handling `POST /api/v1/ledger/debit` and `POST /api/v1/ledger/credit` (with alias support for `/debits` and `/credits`).
**Acceptance criteria:**
- [x] Endpoints validate `@Valid @RequestBody DebitCreditRequestDto`.
- [x] Extracts authenticated user and client IP for audit context.
- [x] Returns HTTP 201 Created with `DebitCreditResponseDto`.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=LedgerMutationControllerTest`
**Dependencies:** Tasks 2.3, 3.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/controller/LedgerMutationController.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/controller/LedgerMutationControllerTest.java`
**Estimated scope:** Small (2 files)

---

### Task 3.3: Atomic Fund Transfer Controller
**Description:** Implement `LedgerTransferController` handling `POST /api/v1/ledger/transfer` (with alias support for `/transfers`).
**Acceptance criteria:**
- [x] Endpoint validates `@Valid @RequestBody TransferRequestDto`.
- [x] Enforces mandatory `Idempotency-Key` header via existing interceptor.
- [x] Returns HTTP 201 Created with `TransferResponseDto`.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=LedgerTransferControllerTest`
**Dependencies:** Tasks 2.3, 3.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/controller/LedgerTransferController.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/controller/LedgerTransferControllerTest.java`
**Estimated scope:** Small (2 files)

---

### Task 3.4: High-Speed Cached Balance Controller
**Description:** Implement `BalanceController` handling `GET /api/v1/ledger/balance/{accountId}` (with alias support for `/accounts/{accountId}/balance`).
**Acceptance criteria:**
- [x] Returns HTTP 200 OK with `BalanceResponseDto` (`accountId`, `currency`, `availableBalance`, `asOfTimestamp`, `isCached`).
- [x] Caches balance in Redis for fast query retrieval ($< 2\text{ms}$).
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=BalanceControllerTest`
**Dependencies:** Tasks 2.2, 3.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/controller/BalanceController.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/controller/BalanceControllerTest.java`
**Estimated scope:** Small (2 files)

---

### Checkpoint: REST Layer Verified
- [x] All REST endpoints exposed and functioning.
- [x] Valid requests succeed with 200 OK / 201 Created.
- [x] Validation errors return RFC-7807 problem details.
- [x] MockMvc tests pass with standalone / MockMvc setup.

---

## Phase 4: Distributed Idempotency Caching & Response Replay

### Task 4.1: Response Caching Wrapper Filter
**Description:** Add `ContentCachingResponseWrapperFilter` so downstream interceptors can access and cache the response body of successful 2xx responses.
**Acceptance criteria:**
- [x] Wraps `HttpServletResponse` in `ContentCachingResponseWrapper`.
- [x] Copies cached response content to original output stream upon completion.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=ContentCachingResponseWrapperFilterTest`
**Dependencies:** None
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/interceptor/ContentCachingResponseWrapperFilter.java`
**Estimated scope:** Small (1 file)

---

### Task 4.2: Idempotency Key 24h Response Replay & Error Cleanup
**Description:** Enhance `IdempotencyInterceptor` to cache successful 2xx responses in Redis (24h TTL) and replay cached responses with `X-Cache-Replay: true` on duplicate requests; release in-flight locks if mutation aborts.
**Acceptance criteria:**
- [x] In `preHandle`: if key exists with status `COMPLETED`, immediately replay cached HTTP response with `X-Cache-Replay: true` without invoking controller.
- [x] In `afterCompletion`: if HTTP status is 2xx, save response body and status to Redis with key `idemp:<key>` and 24-hour TTL.
- [x] In `afterCompletion`: if request resulted in error/exception, delete in-flight Redis lock so client can retry.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=IdempotencyInterceptorTest`
**Dependencies:** Task 4.1
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/interceptor/IdempotencyInterceptor.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/interceptor/IdempotencyInterceptorTest.java`
**Estimated scope:** Small (2 files)

---

### Checkpoint: Distributed Idempotency Replay Verified
- [x] Duplicate request within 24h replays identical response with `X-Cache-Replay: true`.
- [x] Database is touched exactly once across duplicate submissions.
- [x] Failed requests release lock for subsequent retry.

---

## Phase 5: Asynchronous Kafka Domain Event Publishing

### Task 5.1: Spring Kafka Dependency & Broker Configuration
**Description:** Add `spring-kafka` to `pom.xml` and configure Kafka producer settings in `application.properties`.
**Acceptance criteria:**
- [x] `spring-kafka` added to `pom.xml`.
- [x] Producer configuration (`bootstrap-servers`, `key-serializer`, `value-serializer`) added to `application.properties`.
- [x] `KafkaProducerConfig` bean configured with `ProducerFactory` and `KafkaTemplate`.
**Verification:**
- [x] Build succeeds: `./mvnw compile`
**Dependencies:** None
**Files likely touched:**
- `backend/ledger-service/pom.xml`
- `backend/ledger-service/src/main/resources/application.properties`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/config/KafkaProducerConfig.java`
**Estimated scope:** Medium (3 files)

---

### Task 5.2: Transactional Event Publisher
**Description:** Implement `LedgerEventPublisher` publishing `ledger.mutation.completed.v1` and `ledger.transfer.completed.v1` events via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
**Acceptance criteria:**
- [x] Event payloads match Section 6.2 of `API_DESIGN_SPECIFICATION.md`.
- [x] Event published only AFTER database transaction commits.
- [x] Zero events published if transaction rolls back.
**Verification:**
- [x] Tests pass: `./mvnw test -Dtest=LedgerEventPublisherTest`
**Dependencies:** Task 5.1, Task 2.3
**Files likely touched:**
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/event/LedgerMutationEvent.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/event/LedgerTransferEvent.java`
- `backend/ledger-service/src/main/java/com/group2/fse/ledger_service/event/publisher/LedgerEventPublisher.java`
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/event/publisher/LedgerEventPublisherTest.java`
**Estimated scope:** Medium (4 files)

---

### Checkpoint: Domain Event Streaming Verified
- [x] Committed transactions emit Kafka domain events.
- [x] Rolled-back transactions emit nothing.

---

## Phase 6: Comprehensive Concurrency, Deadlock & E2E Validation

### Task 6.1: High-Concurrency Multithreaded Cross-Transfer Stress Tests
**Description:** Implement integration test executing concurrent cross-transfers (Account A $\rightarrow$ B and Account B $\rightarrow$ A) across 50 concurrent threads.
**Acceptance criteria:**
- [ ] Zero deadlocks occur under high concurrency.
- [ ] Sum of balances across Account A and Account B remains strictly invariant ($B_A + B_B = C$).
- [ ] Zero balance leakage or race conditions.
**Verification:**
- [ ] Tests pass: `./mvnw test -Dtest=ConcurrentTransferIntegrationTest`
**Dependencies:** Tasks 2.3, 3.3
**Files likely touched:**
- `backend/ledger-service/src/test/java/com/group2/fse/ledger_service/integration/ConcurrentTransferIntegrationTest.java`
**Estimated scope:** Small (1 file)

---

### Task 6.2: End-to-End Suite Regression & CI/CD Validation
**Description:** Run entire test suite across all modules with datastores active and verify CI workflow readiness.
**Acceptance criteria:**
- [ ] All unit, integration, and security tests pass with 0 failures: `./mvnw test`.
- [ ] Working tree clean, changes ready for PR branch creation.
**Verification:**
- [ ] Tests pass: `./mvnw test`
**Dependencies:** All previous tasks
**Files likely touched:**
- Whole test suite
**Estimated scope:** Validation

---

### Final Checkpoint: Full Ledger Service Complete & Production-Ready
- [ ] All 6 phases executed and verified.
- [ ] Ledger Service completely functional, secure, idempotent, auditable, and event-driven.
