-- SAML 2.0 联邦字段
ALTER TABLE sso_identity_provider ADD COLUMN saml_entity_id VARCHAR(512) DEFAULT NULL;
ALTER TABLE sso_identity_provider ADD COLUMN saml_sso_url VARCHAR(512) DEFAULT NULL;
ALTER TABLE sso_identity_provider ADD COLUMN saml_verification_certificate TEXT DEFAULT NULL;

ALTER TABLE sso_identity_provider MODIFY COLUMN discovery_uri VARCHAR(512) NULL;
ALTER TABLE sso_identity_provider MODIFY COLUMN client_id VARCHAR(256) NULL;
ALTER TABLE sso_identity_provider MODIFY COLUMN client_secret VARCHAR(512) NULL;

-- SCIM 2.0 预置客户端（密钥: change-me-in-production）
INSERT INTO sso_oauth_client (client_id, client_secret_hash, client_name,
    authorization_grant_types, redirect_uris, scopes, require_pkce)
SELECT 'scim-provisioner',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       'SCIM Provisioner',
       'client_credentials',
       '',
       'scim',
       0
WHERE NOT EXISTS (SELECT 1 FROM sso_oauth_client WHERE client_id = 'scim-provisioner');
