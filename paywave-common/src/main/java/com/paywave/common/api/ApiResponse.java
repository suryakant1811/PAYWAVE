package com.paywave.common.api;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.web.CorrelationIds;

public record ApiResponse<T>(boolean success, T data, ErrorBody error, String correlationId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, CorrelationIds.get());
    }

    public static ApiResponse<Void> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code.name(), message), CorrelationIds.get());
    }

    public static ApiResponse<Void> fail(ErrorCode code, String message, String correlationId) {
        return new ApiResponse<>(false, null, new ErrorBody(code.name(), message), correlationId == null ? "" : correlationId);
    }
}
