package com.paywave.web.security;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.security.AuthenticatedUser;
import com.paywave.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SecurityProperties properties, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/internal/") || path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")) {
            return true;
        }
        return properties.getPublicPaths().stream().anyMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        try {
            AuthenticatedUser user = jwtService.parseAccessToken(token);
            List<SimpleGrantedAuthority> authorities = user.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user.userId().toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected access token: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(ErrorCode.UNAUTHORIZED.status());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Access token is invalid or expired"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
