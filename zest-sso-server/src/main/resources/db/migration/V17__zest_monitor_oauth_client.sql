-- 预注册 Zest Monitor Admin OAuth 客户端
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'zest-monitor-admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'Zest Monitor Admin',
       'authorization_code,refresh_token',
       'http://localhost:3000/console/login/callback,http://localhost:8088/console/login/callback',
       'openid,profile,email,roles,tenant',
       1,
       'http://localhost:8088/api/v1/auth/sso/backchannel-logout',
       'http://localhost:8088/console/auth/frontchannel-logout'
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'zest-monitor-admin');
