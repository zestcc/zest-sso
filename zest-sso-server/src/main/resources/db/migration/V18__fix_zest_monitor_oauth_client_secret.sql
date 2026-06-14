-- 修正 zest-monitor-admin 客户端密钥哈希（明文: change-me-in-production，与 V8 一致）
UPDATE sso_oauth_client
SET client_secret_hash = '$2a$10$G1ADy6oi0ALHQCqTyvXHheSM9siuMbhU44YCBt9ZNNUzIiXZ2UzVq'
WHERE client_id = 'zest-monitor-admin';
