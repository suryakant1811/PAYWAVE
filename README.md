# PayWave

PayWave is a Paytm-style wallet and payments backend. A customer can register, hold a wallet, add money through a simulated gateway, pay another customer, pay a merchant, see history, request a refund, and run a merchant settlement.

The application is five backend processes plus an optional **web-ui-service** (static demo UI, no database). There are no Dockerfiles in this repository. Add those yourself when you deploy.

Sample passwords in `.env.example` are for a private development host. This code does not talk to a real bank, card network, or UPI.

## What the application does

A customer signs up and immediately receives a wallet. Adding money, sending money, and paying a merchant all go through one payment flow that updates the wallet and writes a balanced ledger entry. Merchants get a QR identity, can receive payments minus a 1.5% fee, and can be settled for a calendar day. Refunds reverse the money movement before settlement. The customer can read the resulting history and simulated email, SMS, and in-app messages.

## The five services

| Service | Port | Database | What it owns |
| --- | --- | --- | --- |
| api-gateway | 8080 | none | The only public entry point |
| account-service | 8081 | account_db | Sign-in, profile, KYC, and notifications |
| wallet-service | 8082 | wallet_db | Wallet balances and the double-entry ledger |
| payment-service | 8083 | payment_db | Payments, merchants, history, and refunds |
| settlement-service | 8084 | settlement_db | Merchant settlement for one day |
| web-ui-service | 8090 | none | Demo wallet UI (served publicly via gateway `/`) |

Clients call the gateway. Open `http://<host>:8080/` in a browser for the demo UI (register, login, top-up, transfer, history). The root path used to return `UNAUTHORIZED` JSON; the gateway now proxies `/`, `/css/**`, and `/js/**` to web-ui-service without a JWT. The gateway checks the JWT and forwards `/api/v1/auth`, `/api/v1/users`, and `/api/v1/notifications` to account-service; `/api/v1/wallets` and `/api/v1/ledger` to wallet-service; `/api/v1/payments`, `/api/v1/transactions`, `/api/v1/merchants`, and `/api/v1/refunds` to payment-service; and `/api/v1/settlements` to settlement-service.

## Run one service

Install JDK 21 and Maven 3.9, create that service's MySQL 8 database, then:

```bash
cp .env.example .env
set -a && source .env && set +a
export DB_URL='jdbc:mysql://localhost:3306/account_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC'
mvn -pl account-service -am spring-boot:run
```

Repeat with `wallet_db` on port 8082, `payment_db` on port 8083, and `settlement_db` on port 8084. `docker/mysql/init.sh` creates those four databases if you start MySQL from the official image and mount the script.

Leave `PAYWAVE_MESSAGING_MODE` unset. The default is `log`, so Kafka is not required. Set it to `http` when account-service is running and you want wallet, payment, and settlement events delivered to `POST /internal/events`.

Swagger for a service is `http://localhost:<port>/swagger-ui.html`. The gateway does not merge those documents.

```bash
mvn test
mvn -pl wallet-service -am test -Pintegration
```

`mvn test` skips the Testcontainers concurrency test. The integration profile needs Docker only because that test starts MySQL itself.

This repository was written on a machine without Java or Maven, so it has not been compiled here.

## Money rules

- Amounts are INR with two decimal places.
- A wallet balance changes together with a wallet entry, and the payment flow posts a balanced ledger journal for that movement.
- Ledger journals are insert-only. Corrections are new reversal journals.
- The same idempotency key does not move money twice.
- A transfer that cannot credit the receiver credits the sender back.
- Merchant payments keep a 1.5% fee. The merchant wallet receives the net.
- Add-money amounts whose paise are `.13` are declined. Amounts whose paise are `.14` stay pending and do not move money.
- A settled payment cannot be refunded.

## Documentation

- [Architecture](docs/architecture.md)
- [What each service does](docs/services.md)
- [Database design](docs/database.md)
- [API reference](docs/api.md)
- [Local development](docs/local-development.md)
- [Business workflows](docs/workflows.md)
- [Environment variables](docs/environment.md)
- [Testing](docs/testing.md)
