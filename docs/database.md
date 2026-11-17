# Database design

Each running service has one MySQL 8 schema. There are no foreign keys across schemas.

Identifiers are `CHAR(36)` UUIDs. Money is `DECIMAL(19,2)`. Timestamps are `DATETIME(3)` in UTC. Flyway owns the schema. Hibernate uses `ddl-auto: validate`.

| Schema | Tables |
| --- | --- |
| account_db | users, user_roles, refresh_tokens, login_audits, customer_profiles, notifications |
| wallet_db | wallets, wallet_entries, ledger_accounts, journal_entries, journal_lines, outbox_events |
| payment_db | payments, payment_saga_steps, payment_refunds, transactions, merchants, payment_requests, refunds, outbox_events |
| settlement_db | settlements, settlement_lines, outbox_events |

Seeded ledger owners, also declared in `PlatformAccounts`:

| Id | Account |
| --- | --- |
| `00000000-0000-0000-0000-000000000001` | GATEWAY_CLEARING, debit-normal |
| `00000000-0000-0000-0000-000000000002` | FEE_INCOME, credit-normal |
| `00000000-0000-0000-0000-000000000003` | PAYOUT_CLEARING, credit-normal |

Customer and merchant accounts are created when the wallet is opened.

## Posting examples

Add money of 100:

- Debit GATEWAY_CLEARING 100
- Credit CUSTOMER_WALLET 100
- Wallet credit 100

Merchant payment of 100 with a 1.50 fee:

- Debit CUSTOMER_WALLET 100
- Credit MERCHANT_PAYABLE 98.50
- Credit FEE_INCOME 1.50
- Wallet debit customer 100, wallet credit merchant 98.50

Settlement of the 98.50:

- Debit MERCHANT_PAYABLE 98.50
- Credit PAYOUT_CLEARING 98.50
- Wallet debit merchant 98.50

A refund writes a new journal. It does not update the original lines.

Wallet mutation takes `SELECT ... FOR UPDATE` on the wallet row. Outbox readers use `FOR UPDATE SKIP LOCKED`.
