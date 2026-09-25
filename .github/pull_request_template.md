## Description
<!-- Provide a brief summary of the changes introduced in this pull request -->

- **Issue/Ticket ID**: `FSE-___`
- **Module / Microservice**: `ledger-service` / `database` / `infrastructure`
- **Change Type**: `[Feature | Bugfix | Refactor | Performance | Security | Docs]`

---

## Architectural & Invariants Checklist
<!-- Core Banking integrity rules that must NOT be violated -->

- [ ] **Balance Invariant**: Ensured Available Balance never falls below zero ($B_{rem} \ge 0$).
- [ ] **Pessimistic Concurrency**: Row locks acquired in deterministic ascending order (`ORDER BY account_id ASC`) to eliminate deadlocks.
- [ ] **Idempotency Guard**: POST / mutation endpoints enforce atomic Redis SETNX pre-flight checks with `Idempotency-Key` header.
- [ ] **Dual-Write Integrity**: Synchronous write to Oracle master + PostgreSQL audit log preserved with automated rollback upon secondary failure.
- [ ] **Single Source of Truth (SSOT)**: Business balances remain isolated to Oracle XE; PostgreSQL strictly handles immutable audit storage.
- [ ] **SHA-256 Hash Chain**: Audit trigger hash chaining verified and intact.

---

## Testing & Verification
<!-- Describe how these changes were tested -->

- [ ] **Unit Tests**: All unit tests in `src/test/java` pass (`./mvnw test`).
- [ ] **Integration Tests**: Live container test verification completed (`scripts/verify.sh` or `./mvnw test -Dtest=BalanceRepositoryLockTest`).
- [ ] **No Hardcoded Secrets**: Zero plain-text passwords or secret keys checked into version control.
- [ ] **Docker Compose Syntax**: `docker compose config --quiet` passes without errors.

---

## Screenshots / Verification Output (Optional)
<!-- Attach terminal output, surefire test logs, or curl verification if applicable -->
