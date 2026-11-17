package com.paywave.common.messaging;

public final class EventTypes {

    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String WALLET_CREDITED = "WALLET_CREDITED";
    public static final String WALLET_DEBITED = "WALLET_DEBITED";
    public static final String REFUND_COMPLETED = "REFUND_COMPLETED";
    public static final String SETTLEMENT_COMPLETED = "SETTLEMENT_COMPLETED";
    public static final String TOPIC = "paywave.domain-events";

    private EventTypes() {
    }
}
