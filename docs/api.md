# API reference

Public calls go through the gateway at port 8080. Send `Authorization: Bearer <accessToken>` except on register, login, refresh, and logout. Every response uses this envelope:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "correlationId": "..."
}
```

A failure sets `success` to false and fills `error` with `code` and `message`. `data` is null. Common codes: `VALIDATION_ERROR` 400, `UNAUTHORIZED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `CONFLICT` and `INVALID_STATE` and `DUPLICATE_REQUEST` 409, `INSUFFICIENT_BALANCE` and `LIMIT_EXCEEDED` 422, `RATE_LIMITED` 429, `UPSTREAM` 502.

OpenAPI is served by each service at `/swagger-ui.html` and `/v3/api-docs`. Account-service is port 8081, wallet-service 8082, payment-service 8083, settlement-service 8084, and the gateway 8080. The gateway does not combine those documents.

Money fields are decimal strings such as `"100.00"`. Identifiers are UUIDs.

## Auth — no bearer token except `/me`

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | `fullName`, `email`, `phone` (10–15 digits), `password` (8–72 chars, letter and digit) | 201, `userId`, `accessToken`, `refreshToken`, `expiresInSeconds` |
| POST | `/api/v1/auth/login` | `email`, `password` | tokens |
| POST | `/api/v1/auth/refresh` | `refreshToken` | new token pair; the previous refresh token is revoked |
| POST | `/api/v1/auth/logout` | `refreshToken` | empty data |
| GET | `/api/v1/auth/me` | bearer required | `userId`, `email`, `roles` |

Registration also creates the profile and the customer wallet. A failed later step deletes the pending user and the remote profile or wallet.

## User

| Method | Path | Auth | Body |
| --- | --- | --- | --- |
| GET | `/api/v1/users/me` | bearer | profile |
| PUT | `/api/v1/users/me` | bearer | `fullName`, optional `dateOfBirth`, `addressLine`, `city`, `postalCode` |
| POST | `/api/v1/users/me/kyc` | bearer | `documentType`, `documentNumber` |
| GET | `/api/v1/users/{id}` | self or ADMIN | profile |
| PATCH | `/api/v1/users/{id}/status` | ADMIN | `status` of `ACTIVE`, `SUSPENDED`, or `CLOSED` |

KYC stores a hash and the last four characters. Twelve or more letters and digits verify the profile. Anything else is rejected.

## Wallet

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/wallets/me` | `walletId`, `balance`, `currency`, `status`, limits |
| GET | `/api/v1/wallets/me/entries` | recent entries, query `limit` default 50 |
| GET | `/api/v1/wallets/me/reconciliation` | wallet balance next to the ledger balance |

Credit and debit exist only on `/internal/wallets/**`.

## Payments

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/v1/payments/topup` | `amount`, `idempotencyKey`, optional `note` |
| POST | `/api/v1/payments/transfer` | `toUserId`, `amount`, `idempotencyKey`, optional `note` |
| POST | `/api/v1/payments/merchant` | `merchantId`, `amount`, `idempotencyKey`, optional `note` |
| GET | `/api/v1/payments/{id}` | payment the caller paid or received |
| POST | `/api/v1/payments/{id}/reconcile` | resume a `PENDING` payment |

Amount must be from `1.00` to `100000.00`. The same payer and idempotency key returns the original payment. A different amount for that key is `DUPLICATE_REQUEST`. Sending money to yourself is rejected.

Simulated gateway, used only for top-up:

- Paise `13` (`10.13`) returns `FAILED` and moves no money.
- Paise `14` (`10.14`) stays `PENDING` and moves no money. Reconcile keeps it pending.
- Every other amount succeeds.

## Transactions

`GET /api/v1/transactions` supports `type`, `status`, `direction`, `from`, `to` (ISO-8601 instants), `q`, `page`, `size` (max 100), `sort` (`occurredAt`, `amount`, `status`), and `order` (`asc` or `desc`).

`GET /api/v1/transactions/{id}` returns one row owned by the caller.

## Merchants

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/v1/merchants/register` | `businessName`, `category`, `contactEmail`, `settlementAccountNumber` (8–20 digits) |
| GET | `/api/v1/merchants/me` | |
| PUT | `/api/v1/merchants/me` | `businessName`, `category` |
| GET | `/api/v1/merchants/me/qr` | `payload` and `pngBase64` |
| POST | `/api/v1/merchants/me/payment-requests` | `amount`, optional `note`, `expiresInMinutes` |
| GET | `/api/v1/merchants/me/transactions` | `page`, `size` |
| GET | `/api/v1/merchants/{id}` | public merchant profile |

The settlement account number is stored as a hash and the last four digits.

## Refunds

| Method | Path | Who |
| --- | --- | --- |
| POST | `/api/v1/refunds` | payer. Body: `paymentId`, `amount`, `reason`, `idempotencyKey` |
| GET | `/api/v1/refunds/{id}` | requester, payer, or ADMIN |
| POST | `/api/v1/refunds/{id}/approve` | payer, merchant owner, or ADMIN |
| POST | `/api/v1/refunds/{id}/reject` | merchant owner or ADMIN. Body: `{ "reason": "..." }` |

Amounts at or under 5000 INR are processed in the request call. Larger amounts stay `REQUESTED`. A settled payment cannot be refunded. The amount cannot exceed the remaining unrefunded value.

## Settlements

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/v1/settlements/run` | `periodDate` (`YYYY-MM-DD`). ADMIN may also send `merchantId` |
| GET | `/api/v1/settlements` | history for the caller's merchant |
| GET | `/api/v1/settlements/{id}` | one settlement |
| GET | `/api/v1/settlements/{id}/report` | settlement plus snapshotted lines |

Running the same merchant and date again returns the existing completed settlement.

## Notifications and ledger reads

`GET /api/v1/notifications` lists the caller's simulated messages.

`GET /api/v1/ledger/me` returns the caller's ledger balance. `GET /api/v1/ledger/journals` lists journals that touch the caller's account.

## Health

Every process exposes `GET /actuator/health` without a token.
