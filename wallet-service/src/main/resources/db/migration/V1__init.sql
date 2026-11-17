CREATE TABLE wallets (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    owner_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    daily_limit DECIMAL(19,2) NOT NULL,
    monthly_limit DECIMAL(19,2) NOT NULL,
    per_txn_limit DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_wallets_owner (owner_id),
    KEY idx_wallets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wallet_entries (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    wallet_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(36) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_wallet_entries_idem (idempotency_key),
    KEY idx_wallet_entries_wallet_created (wallet_id, created_at),
    CONSTRAINT fk_wallet_entries_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
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

CREATE TABLE ledger_accounts (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    normal_side VARCHAR(10) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_ledger_account (owner_id, account_type, currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_entries (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(36) NOT NULL,
    reversal_of VARCHAR(36) NULL,
    description VARCHAR(255) NOT NULL,
    posted_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_journal_idem (idempotency_key),
    KEY idx_journal_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_lines (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    journal_entry_id VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    KEY idx_journal_lines_entry (journal_entry_id),
    KEY idx_journal_lines_account (account_id),
    CONSTRAINT fk_journal_lines_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id),
    CONSTRAINT fk_journal_lines_account FOREIGN KEY (account_id) REFERENCES ledger_accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ledger_accounts (id, owner_id, account_type, currency, normal_side, created_at) VALUES
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'GATEWAY_CLEARING', 'INR', 'DEBIT', UTC_TIMESTAMP(3)),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'FEE_INCOME', 'INR', 'CREDIT', UTC_TIMESTAMP(3)),
('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'PAYOUT_CLEARING', 'INR', 'CREDIT', UTC_TIMESTAMP(3));
