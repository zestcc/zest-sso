-- ZestSSO 初始化 Schema
-- 遵循阿里巴巴 Java 开发手册：表名小写+下划线，必备字段 id/create_time/update_time

CREATE TABLE IF NOT EXISTS sso_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(64)     NOT NULL COMMENT '用户名',
    email           VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    password_hash   VARCHAR(128)    NOT NULL COMMENT 'BCrypt 密码哈希',
    display_name    VARCHAR(128)    DEFAULT NULL COMMENT '显示名称',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常 2锁定',
    is_super_admin  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
    last_login_at   DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(64)     DEFAULT NULL COMMENT '最后登录IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSO 用户表';

CREATE TABLE IF NOT EXISTS sso_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    code            VARCHAR(64)     NOT NULL COMMENT '角色编码',
    name            VARCHAR(128)    NOT NULL COMMENT '角色名称',
    description     VARCHAR(256)    DEFAULT NULL COMMENT '描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSO 角色表';

CREATE TABLE IF NOT EXISTS sso_user_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sso_tenant (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    code            VARCHAR(64)     NOT NULL COMMENT '租户编码',
    name            VARCHAR(128)    NOT NULL COMMENT '租户名称',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

CREATE TABLE IF NOT EXISTS sso_user_tenant (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    is_default      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认租户',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tenant (user_id, tenant_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户租户关联';

CREATE TABLE IF NOT EXISTS sso_oauth_client (
    id                      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_id               VARCHAR(128)    NOT NULL COMMENT '客户端ID',
    client_secret_hash      VARCHAR(128)    DEFAULT NULL COMMENT '客户端密钥哈希',
    client_name             VARCHAR(128)    NOT NULL COMMENT '客户端名称',
    client_authentication_methods VARCHAR(256) NOT NULL DEFAULT 'client_secret_basic' COMMENT '认证方式',
    authorization_grant_types VARCHAR(512)  NOT NULL COMMENT '授权类型,逗号分隔',
    redirect_uris           TEXT            DEFAULT NULL COMMENT '回调地址,逗号分隔',
    scopes                  VARCHAR(512)    NOT NULL DEFAULT 'openid,profile' COMMENT '授权范围',
    require_pkce            TINYINT         NOT NULL DEFAULT 1 COMMENT '是否要求PKCE',
    access_token_ttl        INT             NOT NULL DEFAULT 3600 COMMENT 'Access Token TTL(秒)',
    refresh_token_ttl       INT             NOT NULL DEFAULT 86400 COMMENT 'Refresh Token TTL(秒)',
    status                  TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 客户端注册表';

CREATE TABLE IF NOT EXISTS sso_audit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(64)     NOT NULL COMMENT '事件类型',
    actor           VARCHAR(128)    DEFAULT NULL COMMENT '操作者',
    target          VARCHAR(256)    DEFAULT NULL COMMENT '操作对象',
    client_id       VARCHAR(128)    DEFAULT NULL COMMENT '客户端ID',
    ip_address      VARCHAR(64)     DEFAULT NULL COMMENT 'IP地址',
    user_agent      VARCHAR(512)    DEFAULT NULL COMMENT 'User-Agent',
    detail          TEXT            DEFAULT NULL COMMENT '详情JSON',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_event_type (event_type),
    KEY idx_actor (actor),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 初始化角色
INSERT INTO sso_role (code, name, description) VALUES
('SSO_ADMIN', 'SSO 管理员', '系统超级管理员，可管理用户/客户端/租户'),
('SSO_OPERATOR', 'SSO 运维', '可查看审计日志、管理普通用户'),
('USER', '普通用户', '标准 SSO 用户');

-- 初始化默认租户
INSERT INTO sso_tenant (code, name) VALUES
('default', '默认租户');

-- 初始化管理员 (密码: admin123, BCrypt)
INSERT INTO sso_user (username, email, password_hash, display_name, status, is_super_admin)
VALUES ('admin', 'admin@zest.local',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
        '系统管理员', 1, 1);

INSERT INTO sso_user_role (user_id, role_id)
SELECT u.id, r.id FROM sso_user u, sso_role r
WHERE u.username = 'admin' AND r.code = 'SSO_ADMIN';

INSERT INTO sso_user_tenant (user_id, tenant_id, is_default)
SELECT u.id, t.id, 1 FROM sso_user u, sso_tenant t
WHERE u.username = 'admin' AND t.code = 'default';

-- 预注册 ZestFlow / ZestLLM 客户端 (密钥: change-me-in-production)
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce)
VALUES
('zestflow-admin',
 '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
 'ZestFlow Admin',
 'authorization_code,refresh_token,client_credentials',
 'http://localhost:5173/login/callback,http://localhost:8080/api/zestflow/auth/sso/callback',
 'openid,profile,email,roles,tenant',
 1),
('zest-llm-admin',
 '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
 'ZestLLM Admin',
 'authorization_code,refresh_token,client_credentials',
 'http://localhost:5174/login/callback,http://localhost:8081/login/callback',
 'openid,profile,email,roles,tenant',
 1);
