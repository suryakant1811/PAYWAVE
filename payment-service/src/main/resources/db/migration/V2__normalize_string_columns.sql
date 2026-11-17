ALTER TABLE payments
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE transactions
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE merchants
    MODIFY settlement_account_last4 VARCHAR(4) NOT NULL,
    MODIFY settlement_account_hash VARCHAR(64) NOT NULL;
