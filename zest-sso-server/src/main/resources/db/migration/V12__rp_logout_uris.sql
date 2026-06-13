-- 预置 RP Back/Front-Channel Logout URI（ZestFlow / ZestLLM）
UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8080/api/zestflow/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:5173/auth/frontchannel-logout'
WHERE client_id = 'zestflow-admin';

UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8088/api/admin/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:8088/auth/frontchannel-logout'
WHERE client_id = 'zest-llm-admin';
