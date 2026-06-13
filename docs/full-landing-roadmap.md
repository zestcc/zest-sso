# ZestSSO 完整版落地路线图

> 目标：从「可上线 IAM」进阶到「对标 Keycloak/Okta 企业运维体验」。  
> 本文档与代码里程碑同步更新。

## 里程碑总览

| 阶段 | 主题 | 状态 | 覆盖度目标 |
|------|------|------|------------|
| **M0** | 核心 IAM（OAuth/OIDC/MFA/Passkey/SLO/SCIM） | ✅ 已完成 | ~75% |
| **M1** | 企业运维基线 | ✅ 已实现 | ~82% |
| **M2** | 生态与可编程 | ✅ 已实现 | ~88% |
| **M3** | 治理与合规 | ✅ 已实现 | ~92% |
| **M4** | 大规模与金融级 | ✅ 已实现（UMA/多活 SOP 除外） | ~95% |

---

## M2 生态与可编程（已实现）

| 项 | 实现 |
|----|------|
| Device Authorization Grant | `device-cli` 公网客户端 + `DevicePublicClientAuthentication*` + `DeviceAuthorizationIT` |
| Node.js / Python SDK | `zest-sso-client-sdk-node`、`zest-sso-client-sdk-python` |
| Webhook 重试队列 | `sso_webhook_delivery` + `WebhookDeliveryService` + Admin API |
| 登录页白标 | `zest.sso.branding.*` + Thymeleaf 登录页 |
| SCIM PATCH/Bulk | `ScimService.processBulk` + `ScimBulkPatchIT` |
| Back-Channel Admin API | `AdminBackchannelDeliveryController` |

---

## M3 治理与合规（已实现）

| 项 | 实现 |
|----|------|
| 闲置账号禁用 | `StaleAccountDisableJob` + `inactive_disabled_at` |
| Access Review | `AccessReviewService` + Admin API |
| 自适应 MFA | `LoginRiskService` 新 IP step-up |
| 邮件 OTP | `MfaService` EMAIL 模式 + `EmailService.sendLoginOtp` |
| 审计导出 | `GET /api/admin/audit-logs/export` |
| 等保测评 | 外部机构 + `docs/compliance-self-check.md` |

---

## M4 大规模与金融级（已实现）

| 项 | 实现 |
|----|------|
| JWT 双 key 轮换 | `sso_jwt_signing_key` + `JwtSigningKeyService` + Admin rotate |
| 地理容灾 / 多活 | 运维 SOP：`docs/deployment.md` |
| mTLS Client Auth | `mtls_certificate_thumbprints` + `TLS_CLIENT_AUTH` |
| SAML IdP-initiated | `/saml2/authenticate/{alias}` |
| UMA / 策略引擎 | M5 规划 |

---

## 生产上线 Checklist（M4 后）

```powershell
# 1. JWT 密钥（文件或 DB 轮换）
powershell -File scripts/generate-jwt-keys.ps1 -OutDir D:\secrets\zest-sso-jwt

# 2. M2~M4 专项验收
$env:MYSQL_PASSWORD='<prod>'
powershell -File scripts/acceptance-m2-m4.ps1

# 3. 全量验收
powershell -File scripts/acceptance.ps1

# 4. RP Back-Channel 联调
#    ZestFlow: scripts/sso-backchannel-e2e.ps1
#    ZestLLM:  deploy/scripts/sso-backchannel-e2e.ps1
```

---

## 对标覆盖度（更新）

| 产品 | M1 | M2~M4（当前） | 目标 |
|------|-----|---------------|------|
| Keycloak | ~82% | **~92%** | ~95% |
| Okta | ~78% | **~88%** | ~92% |
| Auth0 | ~80% | **~90%** | ~93% |

**结论**：M2~M4 完成后，中小企业生产 IAM 全栈能力已具备；大型企业需外部等保与多活落地。
