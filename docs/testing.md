# Testing

## Unit tests

From the repository root, with JDK 21 and Maven:

```bash
mvn test
```

Surefire skips tests tagged `integration`, so this command does not start Docker.

What the unit tests cover:

| Test | Behaviour |
| --- | --- |
| `MoneyTest` | half-even rounding, fee, proportional fee |
| `JwtServiceTest` | sign and verify an access token |
| `AuthServiceTest` | registration calls profile and wallet, and deletes both when wallet creation fails |
| `UserProfileServiceTest` | profile update and KYC hash |
| `WalletServiceTest` | insufficient balance, replayed credit, debit |
| `LedgerServiceTest` | unbalanced journal rejected, replayed journal id |
| `PaymentServiceTest` | declined top-up does not move money; transfer credit failure reverses the ledger; a successful duplicate does not credit again |
| `NotificationServiceTest` | one event creates in-app, email, and SMS rows |
| `SettlementCalculatorTest` | 100 gross, 1.50 fee, 50 refund leaves 49.25 net |
| `FixedWindowLimiterTest` | the gateway limiter rejects once the window is full |

## Integration test

`WalletConcurrencyIT` starts MySQL with Testcontainers, credits 100, and debits 10 from ten threads. The balance must finish at 0.00. It needs a running Docker daemon:

```bash
mvn -pl wallet-service -am test -Pintegration
```

The `integration` profile replaces the default Surefire filter and runs only tests tagged `integration`.

## End-to-end smoke

After the gateway and the four services are running:

```bash
./scripts/e2e-smoke.sh http://localhost:8080
```

The script registers two customers, adds 1000, repeats that idempotency key, declines a 10.13 top-up, transfers 100, registers a merchant, pays 200, refunds 50, pays 80, reads the payer wallet and history, reads the merchant QR, and runs today's settlement.

## Manual failure checks

```bash
# insufficient balance: transfer more than the wallet holds
# expect HTTP 422 and code INSUFFICIENT_BALANCE

# timeout simulation
curl -s -X POST http://localhost:8080/api/v1/payments/topup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":"10.14","idempotencyKey":"timeout-1"}'
# status stays PENDING and the balance does not change

# duplicate amount mismatch
# repeat an idempotency key with a different amount
# expect HTTP 409 and code DUPLICATE_REQUEST
```

## What was not executed here

The authoring machine had no `java`, `mvn`, or `docker` command. These tests are in the tree so the EC2 host can run them. A green local compile was not produced during development.
