-- 企业运维：Back-Channel 投递追踪
CREATE TABLE IF NOT EXISTS sso_backchannel_logout_delivery (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    principal_name      VARCHAR(128)    NOT NULL COMMENT '登出主体',
    client_id           VARCHAR(128)    NOT NULL COMMENT 'RP 客户端 ID',
    backchannel_uri     VARCHAR(512)    NOT NULL COMMENT '回调地址',
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/DEAD',
    attempt_count       INT             NOT NULL DEFAULT 0,
    last_http_status    INT             DEFAULT NULL,
    last_error          VARCHAR(512)    DEFAULT NULL,
    next_retry_at       DATETIME        DEFAULT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_bc_delivery_status_retry (status, next_retry_at),
    KEY idx_bc_delivery_principal (principal_name, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Back-Channel Logout 投递记录';
