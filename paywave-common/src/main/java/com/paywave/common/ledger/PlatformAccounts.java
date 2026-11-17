package com.paywave.common.ledger;

import java.util.UUID;

/**
 * Well-known ledger owners seeded by the wallet-service Flyway migration.
 * These are platform clearing accounts, not customer or merchant records.
 */
public final class PlatformAccounts {

    public static final UUID GATEWAY_CLEARING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID FEE_INCOME = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID PAYOUT_CLEARING = UUID.fromString("00000000-0000-0000-0000-000000000003");

    public static final String GATEWAY_CLEARING_TYPE = "GATEWAY_CLEARING";
    public static final String FEE_INCOME_TYPE = "FEE_INCOME";
    public static final String PAYOUT_CLEARING_TYPE = "PAYOUT_CLEARING";
    public static final String CUSTOMER_WALLET = "CUSTOMER_WALLET";
    public static final String MERCHANT_PAYABLE = "MERCHANT_PAYABLE";

    private PlatformAccounts() {
    }
}
