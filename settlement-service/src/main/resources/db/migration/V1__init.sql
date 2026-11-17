CREATE TABLE settlements (
    id CHAR(36) NOT NULL PRIMARY KEY,
    merchant_id CHAR(36) NOT NULL,
    owner_user_id CHAR(36) NOT NULL,
    period_date DATE NOT NULL,
    gross_amount DECIMAL(19,2) NOT NULL,
    fee_amount DECIMAL(19,2) NOT NULL,
    refund_amount DECIMAL(19,2) NOT NULL,
    net_amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ledger_posted BIT(1) NOT NULL,
    wallet_debited BIT(1) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_settlement_period (merchant_id, period_date),
    KEY idx_settlement_merchant (merchant_id, period_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settlement_lines (
    id CHAR(36) NOT NULL PRIMARY KEY,
    settlement_id CHAR(36) NOT NULL,
    payment_id CHAR(36) NOT NULL,
    gross_amount DECIMAL(19,2) NOT NULL,
    fee_amount DECIMAL(19,2) NOT NULL,
    refund_amount DECIMAL(19,2) NOT NULL,
    net_amount DECIMAL(19,2) NOT NULL,
    KEY idx_settlement_lines (settlement_id),
    CONSTRAINT fk_settlement_lines FOREIGN KEY (settlement_id) REFERENCES settlements (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbox_events (
    id CHAR(36) NOT NULL PRIMARY KEY,
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
