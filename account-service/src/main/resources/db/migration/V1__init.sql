CREATE TABLE users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked BIT(1) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login_audits (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NULL,
    email VARCHAR(255) NOT NULL,
    success BIT(1) NOT NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_login_audit_email_created (email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_profiles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    date_of_birth DATE NULL,
    address_line VARCHAR(255) NULL,
    city VARCHAR(80) NULL,
    postal_code VARCHAR(16) NULL,
    kyc_status VARCHAR(20) NOT NULL,
    account_status VARCHAR(20) NOT NULL,
    kyc_document_type VARCHAR(32) NULL,
    kyc_document_last4 VARCHAR(4) NULL,
    kyc_document_hash CHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_profiles_email (email),
    KEY idx_profiles_status (account_status, kyc_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    dedup_key VARCHAR(120) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_notification_dedup (dedup_key),
    KEY idx_notification_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
