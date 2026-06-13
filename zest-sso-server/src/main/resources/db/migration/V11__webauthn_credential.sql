-- WebAuthn / Passkey 凭据
CREATE TABLE IF NOT EXISTS sso_webauthn_credential (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    credential_id         VARCHAR(512) NOT NULL COMMENT 'Base64URL credential id',
    credential_record_json TEXT        NOT NULL COMMENT 'webauthn4j CredentialRecord JSON',
    nickname              VARCHAR(128) DEFAULT NULL,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at          DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credential_id (credential_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='WebAuthn 凭据';
