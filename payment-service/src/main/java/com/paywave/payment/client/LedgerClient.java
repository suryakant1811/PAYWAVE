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
import java.util.List;
import java.util.UUID;

@Component
public class LedgerClient {

    private final RestClient client;

    public LedgerClient(InternalRestClientFactory factory, @Value("${paywave.clients.wallet-service-url}") String baseUrl) {
        this.client = factory.create(baseUrl);
    }

    @Retry(name = "ledgerService")
    @CircuitBreaker(name = "ledgerService")
    public UUID post(String idempotencyKey, String referenceType, UUID referenceId, String description, List<Line> lines) {
        try {
            ApiResponse<JournalBody> response = client.post().uri("/internal/ledger/journals")
                    .body(new Post(idempotencyKey, referenceType, referenceId, description, lines))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.success() || response.data() == null) {
                throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service returned an empty journal");
            }
            return response.data().journalId();
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service is unreachable");
        }
    }

    @Retry(name = "ledgerService")
    @CircuitBreaker(name = "ledgerService")
    public void reverse(UUID journalId, String idempotencyKey, String description) {
        try {
            ApiResponse<Object> response = client.post().uri("/internal/ledger/journals/reverse")
                    .body(new Reverse(journalId, idempotencyKey, description))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.success()) {
                throw new PaywaveException(ErrorCode.UPSTREAM, "ledger reversal failed");
            }
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "wallet-service is unreachable");
        }
    }

    public record Line(UUID ownerId, String accountType, String direction, BigDecimal amount, String currency) {}
    public record Post(String idempotencyKey, String referenceType, UUID referenceId, String description, List<Line> lines) {}
    public record Reverse(UUID journalId, String idempotencyKey, String description) {}
    public record JournalBody(UUID journalId) {}
}
