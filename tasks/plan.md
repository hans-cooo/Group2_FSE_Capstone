# Implementation Plan: Production-Ready Ledger Engine Service & Security Perimeter

## Overview
Transform `backend/ledger-service` into an enterprise-grade, high-concurrency, secure Core Ledger Engine microservice in full alignment with the project specifications (`API_DESIGN_SPECIFICATION.md`, `KUBERNETES_CORE_BANKING_ARCHITECTURE_V2.md`, and `ENTERPRISE_ARCHITECTURE_DEFENSE_DOSSIER.md`). 

The implementation covers:
1. **Stateless JWT Security Perimeter & Distributed Blacklist (Epic D / FSE-401, 402, 404, 405)**: RSA256/HMAC JWT validation, Redis token revocation blacklist, RFC-7807 problem details error responses, and CORS filtering.
2. **Double-Entry Atomic Transfer & Concurrency Defense (Epic C / FSE-306)**: Pessimistic write locks with strict account ID ordering to prevent database deadlocks, strict non-negative balance invariant ($B_{\text{rem}} \ge 0$), paired double-entry ledger transactions, and synchronous dual-write auditing to PostgreSQL.
3. **Enterprise REST Controllers & Exception Advice (Epic D / FSE-403)**: Authoritative REST controllers for mutations (`/debit`, `/credit`), transfers (`/transfer`), and cached balance lookups (`/balance/{id}`), with global RFC-7807 exception handling.
4. **Distributed Idempotency & Response Replay (Epic C / FSE-307)**: Redis-backed distributed locks and 24-hour response caching replaying identical responses with `X-Cache-Replay: true` on duplicate submissions.
5. **Asynchronous Kafka Event Streaming (Epic E / FSE-501)**: Transactional event publisher emitting `ledger.mutation.completed.v1` post-commit.
6. **Concurrency & Integration Stress Validation**: Verification of multithreaded cross-transfers and CI/CD compatibility.

---

## Architecture Decisions & Invariants

1. **Microservices Deployment Model**:
   - `ledger-service` is an independent Spring Boot 4.x service containerized and deployed to Kubernetes `core-banking` namespace.
   - It maintains its dedicated Oracle transactional store (`BALANCE`, `TRANSACTION`) and dual-writes compliance records to PostgreSQL (`LEDGER_AUDIT_LOG`).
2. **Financial Math & Immutability**:
   - All balance mutations strictly utilize `BigDecimal` with 4 decimal places (`NUMBER(18, 4)` / `scale = 4`).
   - Zero-overdraft invariant is absolute: $B_{\text{rem}} = B_{\text{curr}} - \text{Amount} \ge 0.0000$.
   - Transfers generate two paired `TRANSACTION` records sharing a unique `referenceNo` (`DEBIT` on source, `CREDIT` on destination).
3. **Deadlock Prevention via Strict Lock Ordering**:
   - When acquiring locks across multiple accounts in a transfer:
     $$\text{lockAcquisitionOrder} = [\min(\text{srcId}, \text{dstId}), \max(\text{srcId}, \text{dstId})]$$
   - Accompanied by Oracle row lock timeout hints (`jakarta.persistence.lock.timeout = 5000ms`) to fail gracefully rather than stall threads.
4. **Dual-Write Rollback Guarantee**:
   - PostgreSQL audit write is executed within the same Spring `@Transactional` boundary before Oracle commit. If PostgreSQL write fails, the entire transaction rolls back; if Oracle rollback occurs, no PostgreSQL commit is made.
   - Remove redundant `PostgresLedgerAuditWriter` and obsolete `CompensationManager` to unify on `DualWriteLedgerAuditService`.
5. **Canonical Security Filter Order**:
   ```
   Client HTTP Request
          │
          ▼
   1. JwtAuthenticationFilter (validates Authorization Bearer token, extracts claims, sets SecurityContext)
          │
          ▼
   2. TokenBlacklistFilter (checks Redis blacklist by jti / token hash; returns 401 if revoked)
          │
          ▼
   3. IdempotencyInterceptor (Spring MVC pre-handle checks Redis for in-flight / cached idempotency key)
          │
          ▼
   4. RestController (Controller execution & method-level security)
          │
          ▼
   5. Idempotency Response Cache (post-handle caches 2xx responses in Redis with 24h TTL)
   ```
6. **Standard RFC-7807 Error Envelope**:
   - All HTTP error responses adhere to `application/problem+json` standard format with `type`, `title`, `status`, `detail`, `instance`, `errorCode`, and `timestamp`.

---

## Phased Master Task List

### Phase 1: Security Perimeter & JWT Authentication Pipeline
- [x] Task 1.1: Security & JJWT Maven Dependencies Setup
- [x] Task 1.2: JWT Utilities & Claims Parser (`JwtTokenProvider`)
- [x] Task 1.3: Stateless JWT Authentication Filter (`JwtAuthenticationFilter`)
- [x] Task 1.4: Distributed Token Blacklist Integration (`TokenBlacklistFilter` & `RedisTokenBlacklistServiceImpl`)
- [x] Task 1.5: RFC-7807 Security Exception Handlers (`CustomAuthenticationEntryPoint`, `CustomAccessDeniedHandler`)
- [x] Task 1.6: SecurityFilterChain & CORS Configuration (`SecurityConfig`, `CorsConfig`)

### Checkpoint 1: Security Perimeter Verified
- [x] Public routes (`/actuator/health`, Swagger docs) accessible without credentials.
- [x] Protected routes (`/api/v1/ledger/**`) reject unauthenticated requests with RFC-7807 401 Unauthorized.
- [x] Revoked tokens in Redis return RFC-7807 401 Token Revoked.
- [x] Valid Bearer tokens establish authenticated `SecurityContext` with extracted authorities.

---

### Phase 2: Double-Entry Atomic Transfer & Concurrency Defense
- [x] Task 2.1: Deadlock-Free Ordered Pessimistic Locking Queries (`BalanceRepository`)
- [x] Task 2.2: DTOs & Validation Contracts (`TransferRequestDto`, `DebitCreditRequestDto`, Responses)
- [x] Task 2.3: Double-Entry Atomic Transfer Service Implementation (`AccountBalanceServiceImpl.executeTransfer`)
- [x] Task 2.4: Dual-Write Audit Cleanup & Consolidation (Retire redundant classes)

### Checkpoint 2: Atomic Transfer & Concurrency Verified
- [x] Transfers debit source and credit destination in single atomic transaction.
- [x] Cross-transfers between Account 1 and 2 do not deadlock.
- [x] Transfers with insufficient funds abort with 422 Unprocessable Entity.
- [x] Dual audit entries successfully written to PostgreSQL `LEDGER_AUDIT_LOG`.

---

### Phase 3: Production REST Controllers & RFC-7807 Global Exception Advice
- [x] Task 3.1: Global Exception Handler Advice (`GlobalExceptionHandler`)
- [x] Task 3.2: Authoritative Balance Mutations Controller (`LedgerMutationController`)
- [x] Task 3.3: Atomic Fund Transfer Controller (`LedgerTransferController`)
- [x] Task 3.4: High-Speed Cached Balance Controller (`BalanceController`)

### Checkpoint 3: REST Layer Verified
- [x] Endpoints `/api/v1/ledger/debit`, `/credit`, `/transfer`, `/balance/{accountId}` functional.
- [x] Both singular and plural endpoints routed properly.
- [x] Validation errors and domain errors return RFC-7807 compliant problem envelopes.

---

### Phase 4: Distributed Idempotency Caching & Response Replay
- [ ] Task 4.1: Response Caching Wrapper Filter (`ContentCachingResponseWrapperFilter`)
- [ ] Task 4.2: Idempotency Key 24h Response Replay & Error Cleanup (`IdempotencyInterceptor`)

### Checkpoint 4: Distributed Idempotency Replay Verified
- [ ] Re-sending same `Idempotency-Key` within 24h replays identical HTTP body and status with `X-Cache-Replay: true`.
- [ ] In-flight locks released immediately if an unexpected exception occurs.

---

### Phase 5: Asynchronous Kafka Domain Event Publishing
- [x] Task 5.1: Spring Kafka Dependency & Broker Configuration
- [x] Task 5.2: Transactional Event Publisher (`LedgerEventPublisher` & `ledger.mutation.completed.v1`)

### Checkpoint 5: Domain Event Streaming Verified
- [x] Event published to Kafka topic upon database commit.
- [x] Zero events emitted when transaction rolls back due to failure.

---

### Phase 6: Comprehensive Concurrency, Deadlock & E2E Validation
- [ ] Task 6.1: High-Concurrency Multithreaded Cross-Transfer Stress Tests
- [ ] Task 6.2: End-to-End Suite Regression & CI/CD Validation

### Final Checkpoint: Microservice Sealed & Ready for Review
- [ ] 100% test pass rate across all unit, integration, and concurrency tests.
- [ ] Zero merge conflicts with `dev` branch.

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
| :--- | :---: | :--- |
| **Spring Security breaks existing MockMvc slice tests** | High | Include `spring-security-test`, annotate MockMvc tests with `@WithMockUser(roles = "TELLER")` or use test security configs. |
| **Host Port 1521 Oracle conflict on Windows** | High | Ensure test profiles and local run configurations reference `ORACLE_PORT=1522`. |
| **Database deadlock under concurrent cross-transfers** | Critical | Enforce strict numeric account ID ordering `min(src, dst)` then `max(src, dst)` prior to acquiring locks. |
| **Partial write in dual-write audit logging** | Critical | Place PostgreSQL append and Oracle mutation in unified Spring `@Transactional` context; abort Oracle if Postgres append fails. |
| **Redis connection loss blocking transactions** | Medium | Implement fast timeout failover and structured exception mapping for Redis calls. |
