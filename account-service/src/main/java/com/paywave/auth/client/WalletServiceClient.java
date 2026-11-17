package com.paywave.auth.client;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.web.http.InternalRestClientFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class WalletServiceClient {

    private final org.springframework.web.client.RestClient client;

    public WalletServiceClient(InternalRestClientFactory factory,
                               @Value("${paywave.clients.wallet-service-url}") String baseUrl) {
        this.client = factory.create(baseUrl);
    }

    @Retry(name = "walletService")
    @CircuitBreaker(name = "walletService")
    public void createWallet(CreateWalletRequest request) {
        invoke(() -> client.post().uri("/internal/wallets").body(request).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    @Retry(name = "walletService")
    @CircuitBreaker(name = "walletService")
    public void deleteWallet(UUID ownerId) {
        invoke(() -> client.delete().uri("/internal/wallets/{ownerId}", ownerId).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    private void invoke(java.util.function.Supplier<ApiResponse<Object>> call) {
        try {
            ApiResponse<Object> response = call.get();
            if (response == null || !response.success()) {
                throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service returned an unsuccessful response");
            }
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service is unreachable");
        }
    }
}
