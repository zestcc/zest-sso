-- 告警通道与插件 Admin 在线配置
CREATE TABLE sso_alert_channel (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL COMMENT '显示名称',
    channel_key     VARCHAR(64)  NOT NULL COMMENT '通道键: http-webhook/dingtalk-bot/wecom-bot',
    enabled         TINYINT      NOT NULL DEFAULT 1,
    events          JSON         DEFAULT NULL COMMENT '订阅事件列表，空=全部',
    config          JSON         NOT NULL COMMENT '通道配置 JSON',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_alert_channel_key (channel_key),
    KEY idx_alert_channel_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警通道 Admin 配置';

CREATE TABLE sso_plugin_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plugin_key      VARCHAR(64)  NOT NULL COMMENT '插件键: aliyun-sms/tencent-sms 等',
    enabled         TINYINT      NOT NULL DEFAULT 0,
    config          JSON         DEFAULT NULL COMMENT '插件凭据与参数',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plugin_key (plugin_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可插拔插件 Admin 配置';
