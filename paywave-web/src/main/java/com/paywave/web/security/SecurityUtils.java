package com.paywave.web.security;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new PaywaveException(ErrorCode.UNAUTHORIZED, "Authentication is required");
        }
        try {
            return UUID.fromString(String.valueOf(authentication.getPrincipal()));
        } catch (IllegalArgumentException ex) {
            throw new PaywaveException(ErrorCode.UNAUTHORIZED, "Authentication is required");
        }
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String expected = "ROLE_" + role;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new PaywaveException(ErrorCode.FORBIDDEN, "Role " + role + " is required");
        }
    }
}
