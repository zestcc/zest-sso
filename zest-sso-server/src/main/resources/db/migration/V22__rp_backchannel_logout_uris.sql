-- 2026-07-05：补齐 RP Back-Channel / Front-Channel Logout URI
UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8088/api/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:8088/auth/frontchannel-logout'
WHERE client_id = 'zest-scheduler-admin'
  AND (backchannel_logout_uri IS NULL OR backchannel_logout_uri = '');

UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8090/api/v1/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:8090/auth/frontchannel-logout'
WHERE client_id = 'zest-search-admin'
  AND (backchannel_logout_uri IS NULL OR backchannel_logout_uri = '');

INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce,
    backchannel_logout_uri, frontchannel_logout_uri)
SELECT 'zest-connect-console',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'ZestConnect Console',
       'authorization_code,refresh_token',
       'http://127.0.0.1:10101/console/login/callback,http://localhost:5176/console/login/callback',
       'openid,profile,email,roles,tenant',
       1,
       'http://127.0.0.1:10101/api/v1/auth/sso/backchannel-logout',
       'http://127.0.0.1:10101/console/auth/frontchannel-logout'
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'zest-connect-console');
