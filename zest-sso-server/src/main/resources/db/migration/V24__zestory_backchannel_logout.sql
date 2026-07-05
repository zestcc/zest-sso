-- 2026-07-05：Zestory Admin Back-Channel Logout URI
UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8091/api/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:8091/auth/frontchannel-logout'
WHERE client_id = 'zestory-admin'
  AND (backchannel_logout_uri IS NULL OR backchannel_logout_uri = '');
