#!/usr/bin/env bash
# End-to-end smoke test against a running PayWave gateway.
# Usage: ./scripts/e2e-smoke.sh http://localhost:8080
set -euo pipefail
BASE="${1:-http://localhost:8080}"
python3 - "$BASE" << 'PY'
import json, sys, urllib.error, urllib.request, uuid

base = sys.argv[1].rstrip("/")

def call(method, path, body=None, token=None, expect=200):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Correlation-Id", str(uuid.uuid4()))
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            payload = json.loads(res.read().decode() or "{}")
            status = res.status
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        print(f"FAIL {method} {path} -> {exc.code} {raw}")
        sys.exit(1)
    if status != expect or not payload.get("success"):
        print(f"FAIL {method} {path} -> {status} {payload}")
        sys.exit(1)
    print(f"OK   {method} {path}")
    return payload["data"]

def register(name, email, phone):
    data = call("POST", "/api/v1/auth/register", {
        "fullName": name, "email": email, "phone": phone, "password": "Secret123"
    }, expect=201)
    return data["userId"], data["accessToken"]

n = uuid.uuid4().int % 100000000
suffix = f"{n:08d}"
payer_id, payer = register("Ada Lovelace", f"ada.{suffix}@paywave.test", "90" + suffix)
payee_id, payee = register("Grace Hopper", f"grace.{suffix}@paywave.test", "91" + suffix)

topup = call("POST", "/api/v1/payments/topup", {
    "amount": "1000.00", "idempotencyKey": "topup-" + suffix, "note": "add money"
}, payer)
assert topup["status"] == "SUCCESS", topup

duplicate = call("POST", "/api/v1/payments/topup", {
    "amount": "1000.00", "idempotencyKey": "topup-" + suffix, "note": "add money"
}, payer)
assert duplicate["paymentId"] == topup["paymentId"]

declined = call("POST", "/api/v1/payments/topup", {
    "amount": "10.13", "idempotencyKey": "decline-" + suffix
}, payer)
assert declined["status"] == "FAILED", declined

transfer = call("POST", "/api/v1/payments/transfer", {
    "toUserId": payee_id, "amount": "100.00", "idempotencyKey": "p2p-" + suffix, "note": "thanks"
}, payer)
assert transfer["status"] == "SUCCESS", transfer

merchant = call("POST", "/api/v1/merchants/register", {
    "businessName": "Hopper Stores", "category": "Retail",
    "contactEmail": f"store.{suffix}@paywave.test", "settlementAccountNumber": "123456789012"
}, payee)
payment = call("POST", "/api/v1/payments/merchant", {
    "merchantId": merchant["merchantId"], "amount": "200.00", "idempotencyKey": "merchant-" + suffix
}, payer)
assert payment["status"] == "SUCCESS", payment

refund = call("POST", "/api/v1/refunds", {
    "paymentId": payment["paymentId"], "amount": "50.00", "reason": "partial", "idempotencyKey": "refund-" + suffix
}, payer)
assert refund["status"] == "SUCCESS", refund

second = call("POST", "/api/v1/payments/merchant", {
    "merchantId": merchant["merchantId"], "amount": "80.00", "idempotencyKey": "merchant2-" + suffix
}, payer)
assert second["status"] == "SUCCESS", second

wallet = call("GET", "/api/v1/wallets/me", token=payer)
history = call("GET", "/api/v1/transactions?size=20", token=payer)
assert history["totalElements"] >= 3, history
qr = call("GET", "/api/v1/merchants/me/qr", token=payee)
assert qr["payload"].startswith("paywave://pay?merchantId=")

from datetime import date
settlement = call("POST", "/api/v1/settlements/run", {"periodDate": date.today().isoformat()}, payee)
assert settlement["status"] == "COMPLETED", settlement
report = call("GET", f"/api/v1/settlements/{settlement['settlementId']}/report", token=payee)
print("payer wallet", wallet["balance"])
print("settlement net", settlement["netAmount"])
print("report lines", len(report["lines"]))
print("SMOKE PASSED", suffix)
PY
