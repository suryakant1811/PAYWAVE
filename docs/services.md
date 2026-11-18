# What each microservice does

## api-gateway

The single public door. It routes each `/api/v1` prefix to one of the four applications, checks the JWT, applies CORS, copies or creates `X-Correlation-Id`, limits requests per IP, and turns gateway failures into the same JSON envelope the services use. Register, login, refresh, and logout are public. The HTML demo UI at `/`, `/css/**`, and `/js/**` is proxied to **web-ui-service** (port 8090) and is also public. Everything under `/internal/**` is denied here.

## web-ui-service

Bare-minimum browser demo for stakeholders. Serves static HTML/JS only. The UI calls the same APIs through the gateway with the JWT stored in the browser session. Heap footprint is small (~128 MB). No MySQL.

## account-service

Who the customer is, and how PayWave talks to them.

- Registers a customer and hashes the password with BCrypt.
- Issues a 15-minute JWT and an opaque refresh token. Only the SHA-256 hash of the refresh token is stored. Refresh rotates it. Logout revokes it.
- Creates the customer profile during registration and opens the wallet by calling wallet-service.
- Stores name, address, account status, and simulated KYC. The full document number is never stored.
- Adds the `MERCHANT` role when payment-service registers a merchant.
- Stores simulated in-app, email, and SMS notifications. Other services deliver events to `POST /internal/events`.

## wallet-service

Where the money sits, and the accounting record of it.

- Opens a customer or merchant wallet and returns the balance.
- Credits and debits under a row lock, with daily, monthly, and per-transaction limits.
- Replaying an idempotency key returns the original entry and does not move the balance again.
- Keeps the double-entry ledger in the same database: customer wallets, merchant payables, gateway clearing, fee income, and payout clearing.
- Rejects a journal whose debits and credits do not match. Final journals are not updated or deleted. A correction is a new reversal journal.
- Compares the wallet balance with the ledger balance for reconciliation.
- The public API can read the wallet and the ledger. Credit, debit, and journal posting are internal.

## payment-service

How money moves.

- Adds money through a simulated gateway. Paise `.13` fails with no movement. Paise `.14` stays pending with no movement.
- Transfers between two customers. If the receiver cannot be credited, the sender is credited back and the ledger is reversed.
- Registers a merchant, stores a hash of the settlement account, and returns a QR payload `paywave://pay?merchantId={id}`.
- Pays a merchant and keeps a 1.5% fee in the fee-income account.
- Stores transaction history and supports filter, search, and pagination.
- Accepts refunds, moves the money back, and posts the reversing journal. Amounts up to 5000 INR are processed immediately. Larger amounts wait for approval. A settled payment cannot be refunded.

## settlement-service

Paying the merchant for one calendar day.

- Loads that merchant's unsettled payments from payment-service.
- Snapshots the gross, fee, refund, and remaining net.
- Debits the merchant wallet and posts merchant-payable against payout clearing in wallet-service.
- Marks those payments settled so they are not paid twice.
- Running the same merchant and date again returns the completed settlement.
