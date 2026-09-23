-- ==============================================================================
-- 03_seed_data.sql: Baseline Reference & Seed Data for Oracle Database
-- ==============================================================================

ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = CORE_USER;

-- 1. SEED ROLES
INSERT INTO ROLE (role_name) VALUES ('ROLE_ADMIN');
INSERT INTO ROLE (role_name) VALUES ('ROLE_TELLER');
INSERT INTO ROLE (role_name) VALUES ('ROLE_CUSTOMER');

-- 2. SEED INTERNAL USERS (BCrypt hash for 'Password123!')
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO "USER" (role_id, username, password_hash, email, status)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@corebank.local', 'ACTIVE');

INSERT INTO "USER" (role_id, username, password_hash, email, status)
VALUES (2, 'teller_alice', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'alice@corebank.local', 'ACTIVE');

INSERT INTO "USER" (role_id, username, password_hash, email, status)
VALUES (2, 'teller_bob', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'bob@corebank.local', 'ACTIVE');

-- 3. SEED CUSTOMERS
INSERT INTO CUSTOMER (username, password_hash, email, kyc_status)
VALUES ('john_doe', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'john.doe@example.com', 'VERIFIED');

INSERT INTO CUSTOMER (username, password_hash, email, kyc_status)
VALUES ('maria_santos', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'maria.santos@example.com', 'VERIFIED');

INSERT INTO CUSTOMER (username, password_hash, email, kyc_status)
VALUES ('david_kim', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'david.kim@example.com', 'VERIFIED');

-- 4. SEED KYC
INSERT INTO KYC (customer_id, first_name, middle_initial, last_name, address, civil_status, occupation, mobile_number, status)
VALUES (1, 'John', 'D', 'Doe', '123 Ayala Avenue, Makati City, Metro Manila', 'SINGLE', 'Software Engineer', '+639171234567', 'VERIFIED');

INSERT INTO KYC (customer_id, first_name, middle_initial, last_name, address, civil_status, occupation, mobile_number, status)
VALUES (2, 'Maria', 'S', 'Santos', '456 BGC Boulevard, Taguig City, Metro Manila', 'MARRIED', 'Finance Manager', '+639189876543', 'VERIFIED');

INSERT INTO KYC (customer_id, first_name, middle_initial, last_name, address, civil_status, occupation, mobile_number, status)
VALUES (3, 'David', 'K', 'Kim', '789 Ortigas Center, Pasig City, Metro Manila', 'SINGLE', 'Data Architect', '+639205551234', 'VERIFIED');

-- 5. SEED ACCOUNTS
INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, status)
VALUES (1, 'ACC_10000001', 'SAVINGS', 'PHP', 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, status)
VALUES (2, 'ACC_10000002', 'CHECKING', 'PHP', 'ACTIVE');

INSERT INTO ACCOUNT (customer_id, account_number, account_type, currency, status)
VALUES (3, 'ACC_10000003', 'SAVINGS', 'PHP', 'ACTIVE');

-- 6. SEED BALANCES (Baseline starting capital)
INSERT INTO BALANCE (account_id, available_balance) VALUES (1, 50000.0000);
INSERT INTO BALANCE (account_id, available_balance) VALUES (2, 25000.0000);
INSERT INTO BALANCE (account_id, available_balance) VALUES (3, 100000.0000);

-- 7. SEED INITIAL OPENING TRANSACTIONS
INSERT INTO "TRANSACTION" (account_id, transaction_type, amount, previous_balance, new_balance, status, approved_by)
VALUES (1, 'INITIAL_DEPOSIT', 50000.0000, 0.0000, 50000.0000, 'COMPLETED', 1);

INSERT INTO "TRANSACTION" (account_id, transaction_type, amount, previous_balance, new_balance, status, approved_by)
VALUES (2, 'INITIAL_DEPOSIT', 25000.0000, 0.0000, 25000.0000, 'COMPLETED', 1);

INSERT INTO "TRANSACTION" (account_id, transaction_type, amount, previous_balance, new_balance, status, approved_by)
VALUES (3, 'INITIAL_DEPOSIT', 100000.0000, 0.0000, 100000.0000, 'COMPLETED', 1);

-- 8. SEED INITIAL TRANSACTION AUDITS
INSERT INTO TRANSACTION_AUDIT (transaction_id, account_id, transaction_type, amount, old_balance, new_balance)
VALUES (1, 1, 'INITIAL_DEPOSIT', 50000.0000, 0.0000, 50000.0000);

INSERT INTO TRANSACTION_AUDIT (transaction_id, account_id, transaction_type, amount, old_balance, new_balance)
VALUES (2, 2, 'INITIAL_DEPOSIT', 25000.0000, 0.0000, 25000.0000);

INSERT INTO TRANSACTION_AUDIT (transaction_id, account_id, transaction_type, amount, old_balance, new_balance)
VALUES (3, 3, 'INITIAL_DEPOSIT', 100000.0000, 0.0000, 100000.0000);

-- 9. SEED SYSTEM LOG
INSERT INTO SYSTEM_LOG (user_id, action, service_name)
VALUES (1, 'SEED_DATABASE_INITIALIZATION', 'DATA_FOUNDATION_PHASE_1');

COMMIT;
