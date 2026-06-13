-- 租户管理员角色（中型/大型企业多租户场景）
INSERT INTO sso_role (code, name, description)
SELECT 'TENANT_ADMIN', '租户管理员', '可管理本租户内的用户与组'
WHERE NOT EXISTS (SELECT 1 FROM sso_role WHERE code = 'TENANT_ADMIN');
