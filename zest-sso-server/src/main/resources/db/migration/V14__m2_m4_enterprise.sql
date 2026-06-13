-- M2~M4 企业生产级能力

-- Webhook 投递追踪（对标 Okta Event Hooks 可靠性）
CREATE TABLE IF NOT EXISTS sso_webhook_delivery (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(64)     NOT NULL,
    endpoint_url    VARCHAR(512)    NOT NULL,
    payload_json    TEXT            NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    attempt_count   INT             NOT NULL DEFAULT 0,
    last_http_status INT            DEFAULT NULL,
    last_error      VARCHAR(512)    DEFAULT NULL,
    next_retry_at   DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_webhook_delivery_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- JWT 签名密钥轮换（双 key JWKS，对标 Auth0 / Okta key rollover）
CREATE TABLE IF NOT EXISTS sso_jwt_signing_key (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    key_id          VARCHAR(64)     NOT NULL,
    public_pem      TEXT            NOT NULL,
    private_pem     TEXT            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    not_after       DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jwt_key_id (key_id),
    KEY idx_jwt_key_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 权限复核（Access Review，对标 Okta Access Certification）
CREATE TABLE IF NOT EXISTS sso_access_review_campaign (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(128)    NOT NULL,
    description     VARCHAR(512)    DEFAULT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    due_at          DATETIME        DEFAULT NULL,
    created_by      VARCHAR(64)     NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sso_access_review_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    campaign_id     BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    username        VARCHAR(64)     NOT NULL,
    role_code       VARCHAR(64)     NOT NULL,
    decision        VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    reviewer        VARCHAR(64)     DEFAULT NULL,
    reviewed_at     DATETIME        DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_campaign (campaign_id),
    KEY idx_review_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- OAuth 客户端 mTLS 证书指纹（FAPI / 金融级）
ALTER TABLE sso_oauth_client
    ADD COLUMN mtls_certificate_thumbprints VARCHAR(1024) DEFAULT NULL COMMENT 'SHA-256 指纹,逗号分隔';

-- 用户治理字段
ALTER TABLE sso_user
    ADD COLUMN inactive_disabled_at DATETIME DEFAULT NULL COMMENT '闲置自动禁用时间';

-- Device Authorization 演示客户端（public client，对标 Okta Device Authorization）
INSERT IGNORE INTO sso_oauth_client (
    client_id, client_secret_hash, client_name,
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, require_pkce, require_consent,
    access_token_ttl, refresh_token_ttl, status
) VALUES (
    'device-cli',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'Device CLI Demo',
    'none',
    'urn:ietf:params:oauth:grant-type:device_code,refresh_token',
    'urn:ietf:wg:oauth:2.0:oob',
    'openid,profile',
    0, 0, 3600, 86400, 1
);
