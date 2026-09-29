# CooBS Core Retail Banking Platform — Comprehensive API Endpoint Catalog

This document is the authoritative, production-grade inventory and specification of all **38 core business endpoints** and **5 infrastructure/observability endpoints** across the **CooBS Core Banking Platform**.

---

## 1. Global API Standards & Architecture

### 1.1 Ingress & Routing Architecture
All inbound traffic is routed through **Spring Cloud Gateway (`api-gateway` on port `8080`)**. Direct client access to internal microservices on ports `8081`–`8085` is restricted in production.

```
Client / Frontend (:3000)
    │
    ▼
Spring Cloud API Gateway (:8080)
    │
    ├─► /api/v1/auth/**          ──► auth-service (:8081)
    ├─► /api/v1/accounts/**      ──► account-service (:8082)
    ├─► /api/v1/customers/**     ──► account-service (:8082)
    ├─► /api/v1/ledger/**        ──► ledger-service (:8083)
    ├─► /api/v1/notifications/** ──► notification-service (:8084)
    └─► /api/v1/audit/**         ──► audit-service (:8085)
```

### 1.2 Mandatory & Standard HTTP Headers
| Header | Type | Scope | Description |
| :--- | :---: | :--- | :--- |
| `Authorization` | String | Secured Endpoints | Bearer JWT Token (`Bearer <token>`) containing `sub`, `userId`, `roles`. |
| `X-Correlation-ID` | UUIDv4 | All Requests | Distributed trace identifier passed across all microservice hops. |
| `Idempotency-Key` | UUIDv4 | Financial Mutations | Client-generated key preventing duplicate mutations (`/api/v1/ledger/**`). |
| `Content-Type` | String | Requests with Body | `application/json; charset=UTF-8` |
| `Accept` | String | All Requests | `application/json` or `application/problem+json` |

### 1.3 Redis Edge Rate Limiting Policies
| Route Group | Predicate | Replenish Rate | Burst Capacity | Resolver Type |
| :--- | :--- | :---: | :---: | :--- |
| **Auth** | `/api/v1/auth/**` | **10 req/s** | **20** | Client IP (`ipKeyResolver`) |
| **Ledger** | `/api/v1/ledger/**` | **20 req/s** | **40** | User Principal / IP (`userOrIpKeyResolver`) |
| **Accounts** | `/api/v1/accounts/**` | **50 req/s** | **100** | User Principal / IP (`userOrIpKeyResolver`) |
| **Audit** | `/api/v1/audit/**` | **30 req/s** | **60** | User Principal / IP (`userOrIpKeyResolver`) |
| **Notifications**| `/api/v1/notifications/**`| **50 req/s** | **100** | User Principal / IP (`userOrIpKeyResolver`) |

---

## 2. API Endpoint Master Inventory & Summary

```
Total Core Business Endpoints: 38
├── Auth Service:              8 endpoints
├── Account Service:          13 endpoints
├── Customer & KYC:            7 endpoints
├── Ledger Service:            4 endpoints
├── Notification Service:      3 endpoints
└── Audit Service:             3 endpoints
Total Infrastructure/Actuator:  5 endpoints
GRAND TOTAL:                   43 endpoints
```

---

## 3. Auth Service Endpoints (`auth-service` :8081)

Base Path via Gateway: `/api/v1/auth`

| # | Method | Path | Required Role | Description |
| :-: | :---: | :--- | :---: | :--- |
| 1 | `POST` | `/api/v1/auth/customers/register` | Public | Registers a new retail banking customer and returns JWT credentials. |
| 2 | `POST` | `/api/v1/auth/customers/login` | Public | Authenticates customer with username/password; initiates MFA challenge if enabled. |
| 3 | `POST` | `/api/v1/auth/customers/mfa/verify` | Public | Validates customer MFA OTP token and completes authentication. |
| 4 | `POST` | `/api/v1/auth/staff/login` | Public | Authenticates staff member (TELLER / ADMIN) with username/password. |
| 5 | `POST` | `/api/v1/auth/staff/mfa/verify` | Public | Validates staff MFA OTP token and completes staff authentication. |
| 6 | `POST` | `/api/v1/auth/token/refresh` | Public | Rotates expired access token using valid refresh token cookie/payload. |
| 7 | `POST` | `/api/v1/auth/token/revoke` | Authenticated | Revokes current JWT token and blacklists token ID in Redis. |
| 8 | `GET` | `/api/v1/auth/token/me` | Authenticated | Returns identity, roles, and session metadata for active token holder. |

### Sample Payload: Customer Registration (`POST /api/v1/auth/customers/register`)
```json
{
  "username": "juan.delacruz",
  "password": "Password123!",
  "email": "juan.delacruz@coobs.local",
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "phone": "+639171234567"
}
```

---

## 4. Account Lifecycle Endpoints (`account-service` :8082)

Base Path via Gateway: `/api/v1/accounts`

| # | Method | Path | Required Role | Description |
| :-: | :---: | :--- | :---: | :--- |
| 9 | `POST` | `/api/v1/accounts` | `TELLER`, `ADMIN` | Opens a new bank account (`CHECKING`, `SAVINGS`, `LOAN`) for customer. |
| 10 | `GET` | `/api/v1/accounts` | `TELLER`, `ADMIN` | Staff query listing all bank accounts across the platform. |
| 11 | `GET` | `/api/v1/accounts/my-accounts` | `CUSTOMER`, `TELLER`, `ADMIN` | Retrieves all accounts owned by the authenticated customer. |
| 12 | `GET` | `/api/v1/accounts/{accountId}` | `CUSTOMER`, `TELLER`, `ADMIN` | Fetches details and operational metadata for a specific account ID. |
| 13 | `PATCH` | `/api/v1/accounts/{accountId}/status` | `TELLER`, `ADMIN` | Updates account operational status (`ACTIVE`, `FROZEN`, `DORMANT`, `CLOSED`). |
| 14 | `POST` | `/api/v1/accounts/{accountId}/closure-request` | `CUSTOMER`, `TELLER`, `ADMIN` | Submits formal customer request to terminate and close account. |
| 15 | `GET` | `/api/v1/accounts/{accountId}/closure-request` | `CUSTOMER`, `TELLER`, `ADMIN` | Retrieves active closure request for specific account. |
| 16 | `GET` | `/api/v1/accounts/closure-requests` | `TELLER`, `ADMIN` | Staff inbox listing all pending customer account closure requests. |
| 17 | `POST` | `/api/v1/accounts/closure-requests/{id}/approve` | `TELLER`, `ADMIN` | Staff decision approving account closure and setting status to `CLOSED`. |
| 18 | `POST` | `/api/v1/accounts/closure-requests/{id}/reject` | `TELLER`, `ADMIN` | Staff decision rejecting closure request with explanatory rationale. |
| 19 | `POST` | `/api/v1/accounts/{accountId}/flags` | `TELLER`, `ADMIN` | Places administrative flag on account (`SUSPICIOUS_ACTIVITY`, `LEGAL_HOLD`). |
| 20 | `DELETE`| `/api/v1/accounts/{accountId}/flags/{flagId}` | `TELLER`, `ADMIN` | Clears administrative restriction or compliance flag from account. |
| 21 | `GET` | `/api/v1/accounts/{accountId}/flags` | `TELLER`, `ADMIN` | Lists all active and historical compliance flags on an account. |

### Sample Payload: Open Account (`POST /api/v1/accounts`)
```json
{
  "customerId": 1,
  "accountType": "CHECKING",
  "currency": "PHP",
  "initialDeposit": 1000.0000
}
```

---

## 5. Customer & KYC Endpoints (`account-service` :8082)

Base Path via Gateway: `/api/v1/customers`

| # | Method | Path | Required Role | Description |
| :-: | :---: | :--- | :---: | :--- |
| 22 | `GET` | `/api/v1/customers/me` | `CUSTOMER`, `TELLER`, `ADMIN` | Fetches profile, contact details, and KYC status of authenticated customer. |
| 23 | `GET` | `/api/v1/customers/{customerId}` | `TELLER`, `ADMIN` | Staff lookup to retrieve detailed profile of any registered customer. |
| 24 | `POST` | `/api/v1/customers/kyc/submit` | `CUSTOMER` | Initial customer KYC document and identity verification submission. |
| 25 | `POST` | `/api/v1/customers/kyc/update-request` | `CUSTOMER` | Customer request to update sensitive profile data (e.g. legal name, address). |
| 26 | `GET` | `/api/v1/customers/kyc/update-requests` | `TELLER`, `ADMIN` | Staff queue listing pending KYC verification requests. |
| 27 | `POST` | `/api/v1/customers/kyc/update-requests/{id}/approve` | `TELLER`, `ADMIN` | Staff approval updating customer profile and advancing KYC level. |
| 28 | `POST` | `/api/v1/customers/kyc/update-requests/{id}/reject` | `TELLER`, `ADMIN` | Staff rejection of KYC request with reason message. |

---

## 6. Financial Ledger & Balance Mutation Endpoints (`ledger-service` :8083)

Base Path via Gateway: `/api/v1/ledger`

| # | Method | Path | Required Role | Idempotent | Description |
| :-: | :---: | :--- | :---: | :---: | :--- |
| 29 | `POST` | `/api/v1/ledger/transfers`<br>*(alias: `/transfer`)* | `CUSTOMER`, `TELLER`, `ADMIN` | **Yes** | Executes atomic double-entry transfer between source and destination accounts. Enforces pessimistic row locks and publishes Kafka event. |
| 30 | `POST` | `/api/v1/ledger/debits`<br>*(alias: `/debit`)* | `TELLER`, `ADMIN` | **Yes** | Direct ledger debit mutation (withdrawal / fee assessment). |
| 31 | `POST` | `/api/v1/ledger/credits`<br>*(alias: `/credit`)* | `TELLER`, `ADMIN` | **Yes** | Direct ledger credit mutation (deposit / disbursement). |
| 32 | `GET` | `/api/v1/ledger/accounts/{accountId}/balance`<br>*(alias: `/balance/{accountId}`)* | `CUSTOMER`, `TELLER`, `ADMIN` | No | Real-time balance inspection checking Oracle row with Redis caching. |

### Sample Payload: Atomic Transfer (`POST /api/v1/ledger/transfers`)
```json
{
  "sourceAccountId": 1,
  "destinationAccountId": 2,
  "amount": 125.5000,
  "referenceNo": "REF-TRF-20260929-001",
  "idempotencyKey": "c1ae3937-0048-4af9-b202-4f5603e97fcc"
}
```

---

## 7. In-App Notification Endpoints (`notification-service` :8084)

Base Path via Gateway: `/api/v1/notifications`

| # | Method | Path | Required Role | Description |
| :-: | :---: | :--- | :---: | :--- |
| 33 | `GET` | `/api/v1/notifications/my-notifications` | `CUSTOMER`, `TELLER`, `ADMIN` | Paginated in-app notification feed generated from consumed Kafka events. |
| 34 | `PATCH`| `/api/v1/notifications/{id}/read` | `CUSTOMER`, `TELLER`, `ADMIN` | Marks a specific in-app notification as read. |
| 35 | `GET` | `/api/v1/notifications/unread-count` | `CUSTOMER`, `TELLER`, `ADMIN` | Returns integer count of unread notifications for badge display. |

### Sample Response: Notification Feed (`GET /api/v1/notifications/my-notifications`)
```json
{
  "content": [
    {
      "id": "notif-9182",
      "type": "TRANSFER_COMPLETED",
      "message": "Transfer of 125.50 PHP to Account #2 was successful.",
      "read": false,
      "createdAt": "2026-09-29T01:30:20.000Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 8. Forensic Audit & Verification Endpoints (`audit-service` :8085)

Base Path via Gateway: `/api/v1/audit`

| # | Method | Path | Required Role | Description |
| :-: | :---: | :--- | :---: | :--- |
| 36 | `GET` | `/api/v1/audit/accounts/{accountId}/statement` | `CUSTOMER`, `TELLER`, `ADMIN` | Paginated immutable historical statement retrieved from PostgreSQL audit store. |
| 37 | `GET` | `/api/v1/audit/transactions/{transactionId}` | `CUSTOMER`, `TELLER`, `ADMIN` | Deep forensic audit record with actor ID, client IP, timestamp, and hash. |
| 38 | `GET` | `/api/v1/audit/verify-chain/{accountId}` | `CUSTOMER`, `TELLER`, `ADMIN`, `AUDITOR` | Traverses account audit records and recomputes SHA-256 hashes to verify chain integrity. |

### Sample Response: Cryptographic Verification (`GET /api/v1/audit/verify-chain/1`)
```json
{
  "accountId": 1,
  "totalRecordsVerified": 515,
  "isChainIntact": true,
  "latestHash": "7a3fb7466d8c7ac80049048c0065dcd2054de3a664b6c1d2659c2d19274d80f9",
  "message": "Audit chain integrity verified successfully."
}
```

---

## 9. Infrastructure & Observability Endpoints

Accessible via Gateway or direct container ports for automated health probes and scrapers:

| # | Method | Path | Target Container | Purpose |
| :-: | :---: | :--- | :---: | :--- |
| 39 | `GET` | `/actuator/health` | `api-gateway` (:8080) | Liveness & readiness probe for edge gateway and downstream routes. |
| 40 | `GET` | `/actuator/gateway/routes` | `api-gateway` (:8080) | Live dynamic route table showing mapped microservice targets. |
| 41 | `GET` | `/actuator/prometheus` | All Services (`:8080`–`:8085`) | Micrometer Prometheus scrape endpoint consumed every 5s by Prometheus. |
| 42 | `GET` | `/ready` | `loki` (:3100) | Loki cluster and ingester readiness probe. |
| 43 | `GET` | `/targets` | `promtail` (:9080) | Promtail Docker container discovery and scraper state. |

---

## 10. Role-Based Access Control (RBAC) Matrix

| Endpoint Group | `ROLE_CUSTOMER` | `ROLE_TELLER` | `ROLE_ADMIN` | `ROLE_AUDITOR` |
| :--- | :---: | :---: | :---: | :---: |
| **Auth - Self / Token** | Yes | Yes | Yes | Yes |
| **Auth - Staff Login** | No | Yes | Yes | Yes |
| **Accounts - View Own** | Yes | Yes | Yes | Yes |
| **Accounts - Open / Manage All** | No | Yes | Yes | No |
| **Accounts - Closure Requests** | Submit Own | Approve/Reject | Approve/Reject | View |
| **Accounts - Flags & Restrictions**| No | Yes | Yes | View |
| **Customers - KYC Submit** | Yes | No | No | No |
| **Customers - KYC Approve/Reject**| No | Yes | Yes | No |
| **Ledger - Atomic Transfers** | Yes | Yes | Yes | No |
| **Ledger - Direct Debits/Credits** | No | Yes | Yes | No |
| **Notifications - Feed & Read** | Yes (Own) | Yes (Own) | Yes (Own) | Yes (Own) |
| **Audit - Statements & Forensics** | Yes (Own) | Yes | Yes | Yes |
| **Audit - Hash Chain Verification** | Yes (Own) | Yes | Yes | Yes |
