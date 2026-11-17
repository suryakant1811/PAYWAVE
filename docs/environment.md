# Environment variables

Copy `.env.example` to `.env` and export it before starting a service with Maven. Each `application.yml` falls back to the sample value when a variable is absent.

| Variable | Used by | Purpose |
| --- | --- | --- |
| `DB_USERNAME` | every service | Database user. Sample: `paywave`. |
| `DB_PASSWORD` | every service | Database password. Sample: `paywave_dev_password`. |
| `DB_URL` | one service | JDBC URL including that service's schema. |
| `JWT_SECRET` | gateway and every service | HMAC secret, at least 32 bytes. |
| `JWT_ACCESS_MINUTES` | account-service | Access token lifetime. Default 15. |
| `JWT_REFRESH_DAYS` | account-service | Refresh token lifetime. Default 7. |
| `INTERNAL_API_KEY` | every service | `X-Internal-Api-Key` for `/internal/**`. |
| `CORS_ORIGINS` | gateway | Allowed origin pattern. Sample: `*`. |
| `PAYWAVE_MESSAGING_MODE` | wallet, payment, settlement, account | `log`, `http`, or `kafka`. Default `log`. |
| `KAFKA_BOOTSTRAP_SERVERS` | publishers and account-service | Required only when mode is `kafka`. |
| `ACCOUNT_SERVICE_URL` | gateway, payment-service, and event publishers | Default `http://localhost:8081`. |
| `WALLET_SERVICE_URL` | gateway, account, payment, settlement | Default `http://localhost:8082`. Ledger calls use this same URL. |
| `PAYMENT_SERVICE_URL` | gateway and settlement-service | Default `http://localhost:8083`. Merchant and history calls use this same URL. |
| `SETTLEMENT_SERVICE_URL` | gateway | Default `http://localhost:8084`. |
| `NOTIFICATION_SERVICE_URL` | wallet, payment, settlement | Where outbox events are posted in `http` mode. Default `http://localhost:8081`, because account-service stores notifications. |

Wallet limits default to 100000 daily debit, 500000 monthly top-up, and 50000 per transaction. Override `paywave.wallet.daily-limit`, `paywave.wallet.monthly-limit`, and `paywave.wallet.per-transaction-limit`.

Refund auto-approval defaults to 5000 INR through `paywave.refunds.auto-approve-threshold`.

The merchant fee rate is `paywave.payments.merchant-fee-rate` and defaults to `0.015`.

Do not commit `.env`. It is listed in `.gitignore`.
