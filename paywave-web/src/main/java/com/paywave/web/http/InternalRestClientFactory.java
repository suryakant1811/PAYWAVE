package com.paywave.web.http;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.web.CorrelationIds;
import com.paywave.web.security.InternalApiKeyFilter;
import com.paywave.web.security.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class InternalRestClientFactory {

    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    public InternalRestClientFactory(ObjectMapper objectMapper, SecurityProperties securityProperties) {
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
    }

    public RestClient create(String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(converter -> true);
                    org.springframework.http.converter.json.MappingJackson2HttpMessageConverter jackson =
                            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);
                    converters.add(jackson);
                })
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(InternalApiKeyFilter.HEADER, securityProperties.getInternalApiKey());
                    String correlationId = CorrelationIds.get();
                    if (!correlationId.isBlank()) {
                        request.getHeaders().set(CorrelationIds.HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String raw = "";
                    try {
                        raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                        raw = "";
                    }
                    throw translate(baseUrl, response.getStatusCode().value(), raw);
                })
                .build();
    }

    public PaywaveException translate(String service, int status, String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            String code = node.path("error").path("code").asText("");
            String message = node.path("error").path("message").asText("");
            if (!code.isBlank()) {
                ErrorCode mapped = ErrorCode.valueOf(code);
                return new PaywaveException(mapped, message.isBlank() ? mapped.defaultMessage() : message);
            }
        } catch (IllegalArgumentException ignored) {
            return new PaywaveException(ErrorCode.UPSTREAM, service + " returned " + status);
        } catch (Exception ignored) {
            return new PaywaveException(ErrorCode.UPSTREAM, service + " returned " + status);
        }
        return new PaywaveException(ErrorCode.UPSTREAM, service + " returned " + status);
    }

    public <T> T data(ApiResponse<T> response, String service) {
        if (response == null || !response.success() || response.data() == null) {
            throw new PaywaveException(ErrorCode.UPSTREAM, service + " returned an empty response");
        }
        return response.data();
    }
}
