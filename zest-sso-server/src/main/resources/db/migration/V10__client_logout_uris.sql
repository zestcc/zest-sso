-- OIDC Back/Front-Channel Logout URI（各 RP 单点登出回调）
ALTER TABLE sso_oauth_client
    ADD COLUMN backchannel_logout_uri VARCHAR(512) DEFAULT NULL COMMENT 'Back-Channel Logout 回调 URL',
    ADD COLUMN frontchannel_logout_uri VARCHAR(512) DEFAULT NULL COMMENT 'Front-Channel Logout 回调 URL';
