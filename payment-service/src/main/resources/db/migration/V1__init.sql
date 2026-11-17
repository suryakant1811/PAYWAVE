CREATE TABLE payments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payer_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    payee_id VARCHAR(36) NOT NULL,
    payee_type VARCHAR(20) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    fee_amount DECIMAL(19,2) NOT NULL,
    net_amount DECIMAL(19,2) NOT NULL,
    refunded_amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    gateway_reference VARCHAR(64) NULL,
    note VARCHAR(255) NULL,
    settled BIT(1) NOT NULL,
    settled_at DATETIME(3) NULL,
    settlement_id VARCHAR(36) NULL,
    history_recorded BIT(1) NOT NULL,
    notify_user_id VARCHAR(36) NULL,
    counterparty_name VARCHAR(120) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_payment_idem (payer_id, idempotency_key),
    KEY idx_payment_payee_settled (payee_id, payment_type, settled, status, created_at),
    KEY idx_payment_payer_created (payer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_saga_steps (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    detail VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_saga_payment (payment_id),
    CONSTRAINT fk_saga_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_refunds (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_payment_refunds_payment (payment_id),
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    published_at DATETIME(3) NULL,
    KEY idx_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transactions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    counterparty_id VARCHAR(36) NULL,
    counterparty_name VARCHAR(120) NULL,
    direction VARCHAR(10) NOT NULL,
    txn_type VARCHAR(32) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_txn_idem (payment_id, owner_id, direction, reference_type),
    KEY idx_txn_owner_time (owner_id, occurred_at),
    KEY idx_txn_owner_type_status (owner_id, txn_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchants (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    business_name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    settlement_account_last4 CHAR(4) NOT NULL,
    settlement_account_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    qr_payload VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_merchant_owner (owner_user_id),
    KEY idx_merchant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_requests (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    merchant_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    note VARCHAR(255) NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_payment_requests_merchant (merchant_id),
    CONSTRAINT fk_payment_requests_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refunds (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    requester_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    fee_component DECIMAL(19,2) NOT NULL,
    net_component DECIMAL(19,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    payment_type VARCHAR(20) NOT NULL,
    payer_id VARCHAR(36) NOT NULL,
    payee_id VARCHAR(36) NOT NULL,
    notify_user_id VARCHAR(36) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_refund_idem (requester_id, idempotency_key),
    KEY idx_refund_payment (payment_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

