-- 本地 RP 联调端口（ZestFlow :8089/:5177，ZestLLM :8090/:5176）
UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8089/api/zestflow/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:5177/auth/frontchannel-logout',
    redirect_uris = CONCAT(
        redirect_uris,
        ',http://localhost:5177/login/callback,http://localhost:8089/api/zestflow/auth/sso/callback'
    )
WHERE client_id = 'zestflow-admin'
  AND redirect_uris NOT LIKE '%localhost:5177%';

UPDATE sso_oauth_client
SET backchannel_logout_uri = 'http://localhost:8090/api/admin/auth/sso/backchannel-logout',
    frontchannel_logout_uri = 'http://localhost:8090/auth/frontchannel-logout',
    redirect_uris = CONCAT(
        redirect_uris,
        ',http://localhost:5176/login/callback,http://localhost:8090/login/callback'
    )
WHERE client_id = 'zest-llm-admin'
  AND redirect_uris NOT LIKE '%localhost:5176%';
