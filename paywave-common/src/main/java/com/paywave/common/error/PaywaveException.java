package com.paywave.common.error;

public class PaywaveException extends RuntimeException {

    private final ErrorCode code;

    public PaywaveException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public PaywaveException(ErrorCode code, String message) {
        super(message == null || message.isBlank() ? code.defaultMessage() : message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
