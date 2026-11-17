package com.paywave.common.error;

public enum ErrorCode {
    VALIDATION_ERROR(400, "Validation failed"),
    UNAUTHORIZED(401, "Authentication is required"),
    FORBIDDEN(403, "You do not have permission to perform this action"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Request conflicts with current state"),
    INVALID_STATE(409, "This operation is not allowed in the current state"),
    DUPLICATE_REQUEST(409, "This request was already processed"),
    INSUFFICIENT_BALANCE(422, "Insufficient wallet balance"),
    LIMIT_EXCEEDED(422, "Wallet limit exceeded"),
    PAYMENT_DECLINED(422, "Payment was declined"),
    ACCOUNT_INACTIVE(403, "Account is not active"),
    UPSTREAM(502, "A downstream service failed"),
    RATE_LIMITED(429, "Too many requests"),
    INTERNAL(500, "Unexpected error");

    private final int status;
    private final String defaultMessage;

    ErrorCode(int status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
