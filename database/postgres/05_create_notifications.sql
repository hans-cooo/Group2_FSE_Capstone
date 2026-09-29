-- ==============================================================================
-- 05_create_notifications.sql: PostgreSQL Notification Store Schema
-- Architecture Role: Asynchronous In-App Notification Feed & Event Tracking
-- Reference: docs/NOTIFICATION_SERVICE_ARCHITECTURE_AND_ENGINEERING_GUIDE.md
-- ==============================================================================

SET search_path TO audit_store, public;

-- ==============================================================================
-- NOTIFICATIONS TABLE
-- Stores multi-channel alerts and in-app notifications for retail banking customers
-- ==============================================================================
CREATE TABLE IF NOT EXISTS audit_store.notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    event_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at TIMESTAMPTZ
);

-- Query performance indexes for customer feeds and deduplication
CREATE INDEX IF NOT EXISTS idx_notifications_customer ON audit_store.notifications(customer_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_event_id ON audit_store.notifications(event_id);
