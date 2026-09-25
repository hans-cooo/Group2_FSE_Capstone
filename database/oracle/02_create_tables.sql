-- ==============================================================================
-- 02_create_tables.sql: DDL Schema for 13 ERD Tables in Oracle XE / 23ai Free
-- Schema: Master System of Record & Pessimistic Locking
-- ==============================================================================

ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = CORE_USER;

-- 1. ROLE TABLE
CREATE TABLE ROLE (
    role_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name VARCHAR2(50) NOT NULL UNIQUE
);

-- 2. USER TABLE (Uses quoted identifier "USER" to avoid Oracle keyword collision)
CREATE TABLE "USER" (
    user_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id NUMBER(19) NOT NULL,
    username VARCHAR2(100) NOT NULL UNIQUE,
    password_hash VARCHAR2(255) NOT NULL,
    email VARCHAR2(150) NOT NULL UNIQUE,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES ROLE(role_id)
);

-- 3. CUSTOMER TABLE
CREATE TABLE CUSTOMER (
    customer_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR2(100) NOT NULL UNIQUE,
    password_hash VARCHAR2(255) NOT NULL,
    email VARCHAR2(150) NOT NULL UNIQUE,
    kyc_status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. KYC TABLE
CREATE TABLE KYC (
    kyc_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id NUMBER(19) NOT NULL UNIQUE,
    first_name VARCHAR2(100) NOT NULL,
    middle_initial VARCHAR2(10),
    last_name VARCHAR2(100) NOT NULL,
    address VARCHAR2(255) NOT NULL,
    civil_status VARCHAR2(50),
    occupation VARCHAR2(100),
    mobile_number VARCHAR2(30) NOT NULL,
    status VARCHAR2(20) DEFAULT 'VERIFIED' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_kyc_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- 5. KYC UPDATE REQUEST TABLE
CREATE TABLE KYC_UPDATE_REQUEST (
    kyc_request_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kyc_id NUMBER(19) NOT NULL,
    new_first_name VARCHAR2(100),
    new_middle_initial VARCHAR2(10),
    new_last_name VARCHAR2(100),
    new_address VARCHAR2(255),
    new_mobile_number VARCHAR2(30),
    new_civil_status VARCHAR2(50),
    new_occupation VARCHAR2(100),
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    approved_by NUMBER(19),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    CONSTRAINT fk_kyc_req_kyc FOREIGN KEY (kyc_id) REFERENCES KYC(kyc_id),
    CONSTRAINT fk_kyc_req_user FOREIGN KEY (approved_by) REFERENCES "USER"(user_id)
);

-- 6. ACCOUNT TABLE
CREATE TABLE ACCOUNT (
    account_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id NUMBER(19) NOT NULL,
    account_number VARCHAR2(34) NOT NULL UNIQUE,
    account_type VARCHAR2(30) NOT NULL,
    currency VARCHAR2(3) DEFAULT 'PHP' NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
);

-- 7. BALANCE TABLE (Target for SELECT ... FOR UPDATE & Pessimistic Locks)
CREATE TABLE BALANCE (
    balance_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id NUMBER(19) NOT NULL UNIQUE,
    available_balance NUMBER(18, 4) DEFAULT 0.0000 NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_balance_account FOREIGN KEY (account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT chk_oracle_balance_non_neg CHECK (available_balance >= 0)
);

-- 8. TRANSFER REQUEST TABLE
CREATE TABLE TRANSFER_REQUEST (
    transfer_request_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_account_id NUMBER(19) NOT NULL,
    destination_account_id NUMBER(19) NOT NULL,
    amount NUMBER(18, 4) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    approved_by NUMBER(19),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    CONSTRAINT fk_transfer_src_acc FOREIGN KEY (source_account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_transfer_dst_acc FOREIGN KEY (destination_account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_transfer_approved_by FOREIGN KEY (approved_by) REFERENCES "USER"(user_id),
    CONSTRAINT chk_oracle_transfer_pos CHECK (amount > 0)
);

-- 9. ACCOUNT FLAG TABLE
CREATE TABLE ACCOUNT_FLAG (
    flag_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id NUMBER(19) NOT NULL,
    reason VARCHAR2(255) NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    flagged_by NUMBER(19) NOT NULL,
    removed_by NUMBER(19),
    flagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    removed_at TIMESTAMP,
    CONSTRAINT fk_acc_flag_account FOREIGN KEY (account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_acc_flag_user FOREIGN KEY (flagged_by) REFERENCES "USER"(user_id),
    CONSTRAINT fk_acc_flag_rem_user FOREIGN KEY (removed_by) REFERENCES "USER"(user_id)
);

-- 10. TRANSACTION TABLE (Uses quoted identifier "TRANSACTION" to avoid SQL keyword collision)
CREATE TABLE "TRANSACTION" (
    transaction_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id NUMBER(19) NOT NULL,
    transaction_type VARCHAR2(30) NOT NULL,
    amount NUMBER(18, 4) NOT NULL,
    previous_balance NUMBER(18, 4) NOT NULL,
    new_balance NUMBER(18, 4) NOT NULL,
    status VARCHAR2(20) DEFAULT 'COMPLETED' NOT NULL,
    approved_by NUMBER(19),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_txn_account FOREIGN KEY (account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_txn_approved_by FOREIGN KEY (approved_by) REFERENCES "USER"(user_id),
    CONSTRAINT chk_oracle_txn_amount_pos CHECK (amount > 0),
    CONSTRAINT chk_oracle_txn_new_bal_non_neg CHECK (new_balance >= 0)
);

-- 11. TRANSACTION FLAG TABLE
CREATE TABLE TRANSACTION_FLAG (
    flag_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id NUMBER(19) NOT NULL,
    reason VARCHAR2(255) NOT NULL,
    status VARCHAR2(20) DEFAULT 'FLAGGED' NOT NULL,
    flagged_by NUMBER(19) NOT NULL,
    removed_by NUMBER(19),
    flagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    removed_at TIMESTAMP,
    CONSTRAINT fk_txn_flag_txn FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id),
    CONSTRAINT fk_txn_flag_user FOREIGN KEY (flagged_by) REFERENCES "USER"(user_id),
    CONSTRAINT fk_txn_flag_rem_user FOREIGN KEY (removed_by) REFERENCES "USER"(user_id)
);

-- 12. SYSTEM LOG TABLE
CREATE TABLE SYSTEM_LOG (
    log_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id NUMBER(19),
    action VARCHAR2(255) NOT NULL,
    service_name VARCHAR2(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_syslog_user FOREIGN KEY (user_id) REFERENCES "USER"(user_id)
);

-- 13. TRANSACTION AUDIT TABLE (Oracle mirror / legacy audit)
CREATE TABLE TRANSACTION_AUDIT (
    audit_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id NUMBER(19) NOT NULL,
    account_id NUMBER(19) NOT NULL,
    transaction_type VARCHAR2(30) NOT NULL,
    amount NUMBER(18, 4) NOT NULL,
    old_balance NUMBER(18, 4) NOT NULL,
    new_balance NUMBER(18, 4) NOT NULL,
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_txnaudit_txn FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id),
    CONSTRAINT fk_txnaudit_account FOREIGN KEY (account_id) REFERENCES ACCOUNT(account_id)
);

-- 14. ACCOUNT CLOSURE REQUEST TABLE
CREATE TABLE ACCOUNT_CLOSURE_REQUEST (
    closure_request_id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id NUMBER(19) NOT NULL,
    reason VARCHAR2(255),
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    approved_by NUMBER(19),
    rejection_reason VARCHAR2(255),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    CONSTRAINT fk_closure_req_acc FOREIGN KEY (account_id) REFERENCES ACCOUNT(account_id),
    CONSTRAINT fk_closure_req_user FOREIGN KEY (approved_by) REFERENCES "USER"(user_id)
);

-- PERFORMANCE & QUERY INDEXES (Unique columns already have automatic unique indexes)
CREATE INDEX idx_account_customer ON ACCOUNT(customer_id);
CREATE INDEX idx_txn_account ON "TRANSACTION"(account_id);
CREATE INDEX idx_txn_created_at ON "TRANSACTION"(created_at);
CREATE INDEX idx_transfer_src_dst ON TRANSFER_REQUEST(source_account_id, destination_account_id);
CREATE INDEX idx_closure_req_acc ON ACCOUNT_CLOSURE_REQUEST(account_id);
