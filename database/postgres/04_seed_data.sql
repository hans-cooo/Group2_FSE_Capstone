-- ==============================================================================
-- 04_seed_data.sql: Baseline Reference & Seed Data for Immutable Audit Store
-- Architecture Role: Initial Genesis Audit Rows for Accounts 1, 2, and 3
-- ==============================================================================

SET search_path TO audit_store, public;

-- SEED IMMUTABLE AUDIT STORE
-- Corresponding to initial account deposits committed in Oracle Master (Accounts 1, 2, 3)
-- The trigger trg_compute_hash_chain will compute SHA-256 hashes from the genesis seed
INSERT INTO audit_store.ledger_mutation_audit (
    transaction_id,
    account_id,
    transaction_type,
    amount,
    old_balance,
    new_balance,
    actor_id,
    client_ip
) VALUES
    (1, 1, 'INITIAL_DEPOSIT', 50000.0000, 0.0000, 50000.0000, 1, '127.0.0.1'),
    (2, 2, 'INITIAL_DEPOSIT', 25000.0000, 0.0000, 25000.0000, 1, '127.0.0.1'),
    (3, 3, 'INITIAL_DEPOSIT', 100000.0000, 0.0000, 100000.0000, 1, '127.0.0.1');
