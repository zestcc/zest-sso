-- 2026-07-05：预注册 Zestory Admin OAuth 客户端（密钥: change-me-in-production）
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'zestory-admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'Zestory Admin',
       'authorization_code,refresh_token',
       'http://localhost:8091/login/callback,http://127.0.0.1:8091/login/callback,http://localhost:5273/login/callback',
       'openid,profile,email,roles,tenant',
       1,
       'http://localhost:8091/api/auth/sso/backchannel-logout',
       'http://localhost:8091/auth/frontchannel-logout'
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'zestory-admin');
