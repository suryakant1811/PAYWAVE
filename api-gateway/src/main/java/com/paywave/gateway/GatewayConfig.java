package com.paywave.gateway;

import com.paywave.common.security.AuthenticatedUser;
import com.paywave.common.security.JwtService;
import com.paywave.common.web.CorrelationIds;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Configuration
public class GatewayConfig {

    @Bean
    JwtService jwtService(@Value("${paywave.security.jwt-secret}") String secret,
                          @Value("${paywave.security.access-token-minutes:15}") long minutes) {
        return new JwtService(secret, minutes);
    }

    @Bean
    FixedWindowLimiter fixedWindowLimiter() {
        return new FixedWindowLimiter();
    }

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http, JwtService jwtService, ObjectMapper objectMapper) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, exception) -> write(exchange, objectMapper, 401, "UNAUTHORIZED", "Authentication is required"))
                        .accessDeniedHandler((exchange, exception) -> write(exchange, objectMapper, 403, "FORBIDDEN", "You do not have permission to perform this action")))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .pathMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                        .pathMatchers("/internal/**").denyAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    GlobalFilter correlationFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIds.HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            final String id = correlationId;
            ServerHttpRequest request = exchange.getRequest().mutate().header(CorrelationIds.HEADER, id).build();
            ServerWebExchange mutated = exchange.mutate().request(request).build();
            mutated.getAttributes().put(CorrelationIds.MDC_KEY, id);
            mutated.getResponse().getHeaders().set(CorrelationIds.HEADER, id);
            return chain.filter(mutated);
        };
    }

    @Bean
    GlobalFilter rateLimitFilter(FixedWindowLimiter limiter,
                                 @Value("${paywave.gateway.auth-rate-limit-per-minute:20}") int authLimit,
                                 @Value("${paywave.gateway.rate-limit-per-minute:120}") int defaultLimit) {
        return new OrderedRateLimit(limiter, authLimit, defaultLimit);
    }

    private WebFilter jwtFilter(JwtService jwtService) {
        return (exchange, chain) -> {
            String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }
            try {
                AuthenticatedUser user = jwtService.parseAccessToken(header.substring(7).trim());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user.userId().toString(), null,
                        user.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
                return chain.filter(exchange).contextWrite(org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication(authentication));
            } catch (RuntimeException ex) {
                return chain.filter(exchange);
            }
        };
    }

    static Mono<Void> write(ServerWebExchange exchange, ObjectMapper objectMapper, int status, String code, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = String.valueOf(exchange.getAttributeOrDefault(CorrelationIds.MDC_KEY, ""));
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of(
                    "success", false,
                    "data", "",
                    "error", Map.of("code", code, "message", message),
                    "correlationId", correlationId));
        } catch (JsonProcessingException ex) {
            body = ("{\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    static final class OrderedRateLimit implements GlobalFilter, Ordered {
        private final FixedWindowLimiter limiter;
        private final int authLimit;
        private final int defaultLimit;

        OrderedRateLimit(FixedWindowLimiter limiter, int authLimit, int defaultLimit) {
            this.limiter = limiter;
            this.authLimit = authLimit;
            this.defaultLimit = defaultLimit;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 1;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
            String path = exchange.getRequest().getURI().getPath();
            boolean auth = path.startsWith("/api/v1/auth");
            String ip = clientIp(exchange);
            if (!limiter.allow(ip + (auth ? ":auth" : ":api"), auth ? authLimit : defaultLimit, Instant.now())) {
                return write(exchange, new ObjectMapper(), 429, "RATE_LIMITED", "Too many requests");
            }
            return chain.filter(exchange);
        }

        private String clientIp(ServerWebExchange exchange) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return exchange.getRequest().getRemoteAddress() == null ? "unknown" : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
    }
}
