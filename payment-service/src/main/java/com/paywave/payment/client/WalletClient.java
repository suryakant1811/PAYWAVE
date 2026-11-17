package com.paywave.payment.client;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.web.http.InternalRestClientFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class WalletClient {

    private final RestClient client;

    public WalletClient(InternalRestClientFactory factory, @Value("${paywave.clients.wallet-service-url}") String baseUrl) {
        this.client = factory.create(baseUrl);
    }

    @Retry(name = "walletService")
    @CircuitBreaker(name = "walletService")
    public void credit(Mutation request) {
        post("/internal/wallets/credit", request);
    }

    @Retry(name = "walletService")
    @CircuitBreaker(name = "walletService")
    public void debit(Mutation request) {
        post("/internal/wallets/debit", request);
    }

    private void post(String path, Mutation request) {
        try {
            ApiResponse<Object> response = client.post().uri(path).body(request).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.success()) {
                throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service returned an empty response");
            }
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service is unreachable");
        }
    }

    public record Mutation(UUID ownerId, String ownerType, BigDecimal amount, String idempotencyKey,
                           String referenceType, UUID referenceId, String currency, UUID notifyUserId) {
    }
}
