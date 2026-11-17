# Business workflows

All of these are reachable through the gateway. The smoke script in `scripts/e2e-smoke.sh` runs the happy path plus a declined top-up and an idempotent retry.

## 1. Register

`POST /api/v1/auth/register` creates a pending auth user, then a profile, then a customer wallet. Opening the wallet opens a credit-normal ledger account. The user becomes `ACTIVE` and the response contains tokens. If the wallet call fails, the profile is deleted and the pending user is removed.

## 2. Login

`POST /api/v1/auth/login` checks BCrypt and account status, writes a login audit, and returns an access JWT plus a refresh token. `POST /api/v1/auth/refresh` rotates the refresh token. `POST /api/v1/auth/logout` revokes it.

## 3. Add money

`POST /api/v1/payments/topup` claims the payment, asks the simulated gateway, then posts the ledger journal and credits the wallet. The journal debits gateway clearing and credits the customer account. A repeated idempotency key returns the same payment and does not credit again. `.13` fails with no money movement. `.14` stays pending with no money movement.

## 4. Transfer

`POST /api/v1/payments/transfer` debits the sender under a wallet row lock, posts a balanced journal, then credits the receiver. If the credit fails, the journal is reversed and the sender is credited back. The payment is then `FAILED`. An insufficient balance returns 422 and does not post a journal.

## 5. Merchant registration

`POST /api/v1/merchants/register` stores the merchant, asks account-service to add the `MERCHANT` role, and opens a merchant wallet whose ledger account is a merchant payable. The settlement account number is reduced to a hash and the last four digits. `GET /api/v1/merchants/me/qr` returns the `paywave://pay?merchantId=` payload and a PNG.

## 6. Merchant payment

`POST /api/v1/payments/merchant` debits the customer for the gross amount, credits the merchant wallet for the net, and posts three ledger lines: customer debit, merchant payable credit, and fee-income credit. The fee rate is 1.5 percent, rounded half-even to paise.

## 7. History

Each successful movement stores a history row in payment-service. `GET /api/v1/transactions` filters and pages that history. Recording the same payment, owner, direction, and reference type again is a no-op.

## 8. Refund

`POST /api/v1/refunds` rejects settled payments and amounts above the remaining balance. Up to 5000 INR it runs immediately. A merchant refund credits the customer the gross refund, debits the merchant wallet for the net portion, and posts a reversal-style journal that also returns the proportional fee from fee income. Payment-service then increases `refunded_amount`. A later crash after `SUCCESS` is safe to retry: the refund id is the idempotency key for the wallet and ledger calls.

Approve and reject apply only while the refund is `REQUESTED`.

## 9. Settlement

`POST /api/v1/settlements/run` with today's date collects that merchant's unsettled successful, partially refunded, and refunded payments created before the next day. It snapshots the lines, debits the merchant wallet by the remaining net, posts merchant-payable debit against payout-clearing credit, and marks the payments settled. A second run for the same date does not pay again. A zero net still completes, with no wallet movement.

Example after a 200 payment (fee 3.00, net 197.00), a 50 refund (net returned 49.25), and an 80 payment (fee 1.20, net 78.80): remaining net is 226.55.

## 10. Notifications

Wallet, payment, and settlement write an outbox row for credit, debit, payment completed, payment failed, refund completed, and settlement completed. The relay delivers it. In `http` mode that is a post to account-service, which stores in-app, email, and SMS rows. Read them with `GET /api/v1/notifications`. The relay runs on a short delay, so a brand-new event may not be visible in the same second.

## Failure compensation

| Failure | What remains |
| --- | --- |
| Top-up declined or timed out | Payment row only. Wallet and ledger unchanged. |
| Transfer credit fails | Sender debit is reversed by a credit. Ledger has an original journal and a reversal journal. |
| Merchant credit fails | Same compensation as a transfer. |
| Refund wallet or ledger step fails | Refund row is `FAILED` with a reason. Idempotent keys let a later attempt continue without a double post when the failed step had not committed. |
| Settlement fails after the ledger post | The settlement row remembers `ledgerPosted` and `walletDebited` and resumes on the next run. |
