package com.paywave.auth.service;

import com.paywave.auth.domain.RefreshToken;
import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.repo.RefreshTokenRepository;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.security.JwtService;
import com.paywave.web.security.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final SecureRandom random = new SecureRandom();

    public TokenService(RefreshTokenRepository refreshTokens, JwtService jwtService, SecurityProperties securityProperties) {
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public IssuedTokens issue(UserAccount user) {
        String refresh = newRefreshToken();
        refreshTokens.save(RefreshToken.issue(user.getId(), sha256(refresh),
                Instant.now().plus(securityProperties.getRefreshTokenDays(), ChronoUnit.DAYS)));
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoles());
        return new IssuedTokens(access, refresh, jwtService.accessTokenSeconds());
    }

    @Transactional
    public IssuedTokens rotate(String rawRefreshToken, UserAccount user) {
        RefreshToken current = requireActive(rawRefreshToken);
        if (!current.getUserId().equals(user.getId())) {
            throw new PaywaveException(ErrorCode.UNAUTHORIZED, "Refresh token does not match the user");
        }
        current.revoke();
        return issue(user);
    }

    @Transactional
    public RefreshToken requireActive(String rawRefreshToken) {
        RefreshToken token = refreshTokens.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(() -> new PaywaveException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid"));
        if (!token.isActive(Instant.now())) {
            throw new PaywaveException(ErrorCode.UNAUTHORIZED, "Refresh token is expired or revoked");
        }
        return token;
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    public static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedTokens(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
    }
}
