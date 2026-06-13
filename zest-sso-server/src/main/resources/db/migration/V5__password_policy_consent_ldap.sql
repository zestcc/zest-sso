CREATE TABLE IF NOT EXISTS sso_password_policy (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    min_length              INT         NOT NULL DEFAULT 8,
    require_uppercase       TINYINT     NOT NULL DEFAULT 1,
    require_lowercase       TINYINT     NOT NULL DEFAULT 1,
    require_digit           TINYINT     NOT NULL DEFAULT 1,
    require_special         TINYINT     NOT NULL DEFAULT 0,
    password_history_count  INT         NOT NULL DEFAULT 3,
    max_age_days            INT         NOT NULL DEFAULT 0,
    create_time             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sso_password_policy (min_length, require_uppercase, require_lowercase, require_digit, require_special, password_history_count, max_age_days)
SELECT 8, 1, 1, 1, 0, 3, 0
WHERE NOT EXISTS (SELECT 1 FROM sso_password_policy);

CREATE TABLE IF NOT EXISTS sso_password_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    password_hash   VARCHAR(128)    NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sso_user ADD COLUMN password_changed_at DATETIME DEFAULT NULL;
ALTER TABLE sso_user ADD COLUMN external_id VARCHAR(128) DEFAULT NULL;
ALTER TABLE sso_user ADD COLUMN federation_source VARCHAR(64) DEFAULT NULL;

ALTER TABLE sso_oauth_client ADD COLUMN require_consent TINYINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id    VARCHAR(100)  NOT NULL,
    principal_name          VARCHAR(200)  NOT NULL,
    authorities             VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sso_ldap_provider (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    alias               VARCHAR(64)     NOT NULL,
    display_name        VARCHAR(128)    NOT NULL,
    server_url          VARCHAR(512)    NOT NULL,
    base_dn             VARCHAR(512)    NOT NULL,
    bind_dn             VARCHAR(512)    DEFAULT NULL,
    bind_password       VARCHAR(512)    DEFAULT NULL,
    user_search_base    VARCHAR(512)    NOT NULL,
    user_search_filter  VARCHAR(256)    NOT NULL DEFAULT '(uid={0})',
    group_search_base   VARCHAR(512)    DEFAULT NULL,
    group_role_attribute VARCHAR(128)   DEFAULT 'cn',
    enabled             TINYINT         NOT NULL DEFAULT 1,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ldap_alias (alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sso_identity_provider ADD COLUMN username_claim VARCHAR(128) DEFAULT 'preferred_username';
ALTER TABLE sso_identity_provider ADD COLUMN email_claim VARCHAR(128) DEFAULT 'email';
ALTER TABLE sso_identity_provider ADD COLUMN display_name_claim VARCHAR(128) DEFAULT 'name';
ALTER TABLE sso_identity_provider ADD COLUMN role_claim VARCHAR(128) DEFAULT NULL;
ALTER TABLE sso_identity_provider ADD COLUMN default_role_codes VARCHAR(256) DEFAULT 'USER';
