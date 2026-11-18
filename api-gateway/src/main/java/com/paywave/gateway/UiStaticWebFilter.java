package com.paywave.gateway;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * Serves the demo UI from the gateway JAR before Spring Security runs.
 * Avoids a second process on small EC2 instances and fixes {@code GET /} returning 401 JSON.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UiStaticWebFilter implements WebFilter {

    private final ResourceLoader resourceLoader;

    public UiStaticWebFilter(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        if (!UiPaths.isPublicGet(path)) {
            return chain.filter(exchange);
        }
        String location = UiPaths.classpathResource(path);
        return Mono.fromCallable(() -> read(location))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(payload -> {
                    if (payload == null) {
                        return chain.filter(exchange);
                    }
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.OK);
                    response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
                    response.getHeaders().setContentType(payload.mediaType());
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload.bytes())));
                });
    }

    private Payload read(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists() || !resource.isReadable()) {
            return null;
        }
        byte[] bytes = resource.getInputStream().readAllBytes();
        MediaType type = mediaType(location);
        return new Payload(bytes, type);
    }

    private static MediaType mediaType(String location) {
        if (location.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        if (location.endsWith(".css")) {
            return new MediaType("text", "css");
        }
        if (location.endsWith(".js")) {
            return new MediaType("application", "javascript");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private record Payload(byte[] bytes, MediaType mediaType) {
    }
}