package com.paywave.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public class GatewaySimulator {

    public enum Status { SUCCESS, FAILED, TIMEOUT }

    public record Result(Status status, String reference, String reason) {
        public static Result success(String reference) { return new Result(Status.SUCCESS, reference, null); }
        public static Result failed(String reason) { return new Result(Status.FAILED, null, reason); }
        public static Result timeout() { return new Result(Status.TIMEOUT, null, "SIMULATED_TIMEOUT"); }
    }

    private final int declineCents;
    private final int timeoutCents;

    public GatewaySimulator(int declineCents, int timeoutCents) {
        this.declineCents = declineCents;
        this.timeoutCents = timeoutCents;
    }

    public Result charge(BigDecimal amount) {
        int cents = amount.movePointRight(2).abs().remainder(new BigDecimal("100")).intValue();
        if (cents == declineCents) {
            return Result.failed("SIMULATED_DECLINE");
        }
        if (cents == timeoutCents) {
            return Result.timeout();
        }
        return Result.success("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
