-- MFA 与外部身份源联邦
ALTER TABLE sso_user ADD COLUMN mfa_enabled TINYINT NOT NULL DEFAULT 0;
ALTER TABLE sso_user ADD COLUMN mfa_secret VARCHAR(128) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS sso_identity_provider (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    alias           VARCHAR(64)     NOT NULL COMMENT '提供商标识',
    display_name    VARCHAR(128)    NOT NULL COMMENT '显示名称',
    provider_type   VARCHAR(32)     NOT NULL DEFAULT 'OIDC' COMMENT '类型: OIDC',
    discovery_uri   VARCHAR(512)    NOT NULL COMMENT 'OIDC Discovery URL',
    client_id       VARCHAR(256)    NOT NULL COMMENT 'OAuth Client ID',
    client_secret   VARCHAR(512)    NOT NULL COMMENT 'OAuth Client Secret',
    scopes          VARCHAR(512)    NOT NULL DEFAULT 'openid,profile,email' COMMENT '授权范围',
    enabled         TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idp_alias (alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部身份提供商';
