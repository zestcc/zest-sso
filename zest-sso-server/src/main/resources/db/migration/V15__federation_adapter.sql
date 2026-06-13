-- 联邦身份源插拔适配器：adapter_key + 手动 endpoint JSON
ALTER TABLE sso_identity_provider
    ADD COLUMN adapter_key VARCHAR(64) NOT NULL DEFAULT 'generic-oidc' COMMENT '插拔适配器键: generic-oidc/feishu/dingtalk/wecom',
    ADD COLUMN endpoint_config JSON DEFAULT NULL COMMENT '手动 OAuth2 端点，覆盖 Discovery';
