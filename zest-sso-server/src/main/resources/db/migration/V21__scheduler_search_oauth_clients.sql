-- 2026-07-05：预注册 ZestScheduler / ZestSearch Admin OAuth 客户端（密钥: change-me-in-production）
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'zest-scheduler-admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'ZestScheduler Admin',
       'authorization_code,refresh_token',
       'http://localhost:8088/login/callback,http://localhost:5274/login/callback',
       'openid,profile,email,roles,tenant',
       1,
       NULL,
       NULL
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'zest-scheduler-admin');

INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'zest-search-admin',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'ZestSearch Admin',
       'authorization_code,refresh_token',
       'http://localhost:8090/login/callback,http://localhost:3000/login/callback',
       'openid,profile,email,roles,tenant',
       1,
       NULL,
       NULL
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'zest-search-admin');
