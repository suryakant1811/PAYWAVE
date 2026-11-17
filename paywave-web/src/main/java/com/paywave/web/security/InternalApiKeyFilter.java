package com.paywave.web.security;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public InternalApiKeyFilter(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        String expected = properties.getInternalApiKey() == null ? "" : properties.getInternalApiKey();
        if (!constantTimeEquals(presented, expected)) {
            response.setStatus(ErrorCode.UNAUTHORIZED.status());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Internal API key is missing or invalid"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean constantTimeEquals(String presented, String expected) {
        if (presented == null || expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
