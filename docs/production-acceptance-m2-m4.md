# ZestSSO M2~M4 生产级验收清单

> 对标 Keycloak / Okta / Auth0 企业生产特性。配合 `scripts/acceptance-m2-m4.ps1` 执行。

## M2 生态与可编程

| # | 能力 | 验收标准 | 验证方式 |
|---|------|----------|----------|
| M2-1 | Device Authorization Grant | OIDC Discovery 含 `device_authorization_endpoint`；公网客户端 `device-cli` POST 可获取 `device_code` | `DeviceAuthorizationIT` + `acceptance-m2-m4.ps1` |
| M2-2 | Node.js SDK | PKCE 授权码换票 + UserInfo | `zest-sso-client-sdk-node/index.js` |
| M2-3 | Python SDK | 同等能力 | `zest-sso-client-sdk-python/zest_sso_client.py` |
| M2-4 | Webhook 重试队列 | 表 `sso_webhook_delivery`；失败重试/死信；Admin API | `WebhookDeliveryServiceTest` + `GET /api/admin/webhook-deliveries` |
| M2-5 | 登录白标 / i18n | `zest.sso.branding.*` 驱动登录页标题/副标题 | 访问 `/login?lang=en` |
| M2-6 | SCIM PATCH/Bulk | RFC 7644 Bulk + PATCH | `ScimBulkPatchIT` |
| M2-7 | Back-Channel Admin API | 投递分页 + 手动重试 | `GET /api/admin/logout-deliveries` |

## M3 治理与合规

| # | 能力 | 验收标准 | 验证方式 |
|---|------|----------|----------|
| M3-1 | 闲置账号禁用 | `StaleAccountDisableJob` 超期未登录自动禁用 | 配置 `governance.stale-account-days` |
| M3-2 | Access Review | 创建活动 → 激活 → 审批/撤销 | `POST /api/admin/access-reviews/campaigns` |
| M3-3 | 自适应 MFA | 新 IP 触发 step-up（TOTP 或邮件 OTP） | `LoginRiskService` + 新 IP 登录 |
| M3-4 | 邮件 OTP | 未启用 TOTP 时新设备走邮件验证码 | 日志 `[邮件未启用]` 或 SMTP |
| M3-5 | 审计导出 | CSV 下载 | `GET /api/admin/audit-logs/export` |
| M3-6 | 等保/渗透 | 外部机构 | `docs/compliance-self-check.md` |

## M4 大规模与金融级

| # | 能力 | 验收标准 | 验证方式 |
|---|------|----------|----------|
| M4-1 | JWT 双 key 轮换 | JWKS 含 active + retired key；Admin rotate | `JwtSigningKeyServiceTest` + `POST /api/admin/jwt-keys/rotate` |
| M4-2 | 地理容灾 | MySQL/Redis 跨区 SOP | `docs/deployment.md` 多活章节 |
| M4-3 | mTLS Client Auth | 客户端可配置 `mtls_certificate_thumbprints` + `TLS_CLIENT_AUTH` | Admin 客户端 API |
| M4-4 | SAML IdP-initiated | `/saml2/authenticate/{alias}` | `AdminIdentityProviderIT` |
| M4-5 | UMA/策略引擎 | 路线图占位 | M5 |

## 一键验收

```powershell
$env:MYSQL_PASSWORD = "123456"
mvn -pl zest-sso-server -am test -Pmysql-it -DfailIfNoTests=false
powershell -File scripts/acceptance-m2-m4.ps1
powershell -File scripts/acceptance.ps1
```

## 对标覆盖度（M4 后）

| 产品 | M1 | M2~M4（当前） |
|------|-----|---------------|
| Keycloak | ~82% | **~92%** |
| Okta | ~78% | **~88%** |
| Auth0 | ~80% | **~90%** |

**结论**：M2~M4 完成后，中型企业可生产落地；大型企业仍需外部等保测评与多活运维 SOP。
