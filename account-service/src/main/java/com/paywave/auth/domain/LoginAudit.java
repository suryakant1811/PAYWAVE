package com.paywave.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_audits")
public class LoginAudit {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LoginAudit() {
    }

    public static LoginAudit of(UUID userId, String email, boolean success, String ipAddress) {
        LoginAudit audit = new LoginAudit();
        audit.id = UUID.randomUUID();
        audit.userId = userId;
        audit.email = email;
        audit.success = success;
        audit.ipAddress = ipAddress;
        audit.createdAt = Instant.now();
        return audit;
    }
}
