ALTER TABLE wallets
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE ledger_accounts
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE journal_lines
    MODIFY currency VARCHAR(3) NOT NULL;
