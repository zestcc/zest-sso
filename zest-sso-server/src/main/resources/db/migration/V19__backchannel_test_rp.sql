-- 内置 Back-Channel 联调 RP（回调本机 IdP 测试端点，供 scripts/sso-backchannel-e2e.ps1 使用）
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'backchannel-test-rp',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'Back-Channel Test RP',
       'authorization_code,refresh_token',
       'http://localhost:9000/login/oauth2/code/backchannel-test-rp',
       'openid,profile',
       1,
       'http://localhost:9000/api/public/test/rp/backchannel-logout',
       'http://localhost:9000/admin/'
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'backchannel-test-rp');
