package com.paywave.gateway;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.web.CorrelationIds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;
    public GatewayErrorHandler(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        int status = 500;
        ErrorCode code = ErrorCode.INTERNAL;
        String message = "Unexpected gateway error";
        if (ex instanceof ResponseStatusException response) {
            status = response.getStatusCode().value();
            message = response.getReason() == null ? response.getMessage() : response.getReason();
            code = status == 404 ? ErrorCode.NOT_FOUND : status == 504 ? ErrorCode.UPSTREAM : ErrorCode.INTERNAL;
        } else if (ex instanceof TimeoutException || hasCause(ex, TimeoutException.class)) {
            status = 504;
            code = ErrorCode.UPSTREAM;
            message = "Downstream service timed out";
        }
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = String.valueOf(exchange.getAttributeOrDefault(CorrelationIds.MDC_KEY, ""));
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(ApiResponse.fail(code, message, correlationId));
        } catch (JsonProcessingException encoding) {
            bytes = message.getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (type.isInstance(cursor)) return true;
            cursor = cursor.getCause();
        }
        return false;
    }
}
