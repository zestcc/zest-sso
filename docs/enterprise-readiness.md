# ZestSSO 企业级就绪度说明

## 完整版交付能力（对标 Keycloak / Okta / Auth0）

### 身份认证与协议
- OAuth2/OIDC 授权服务器（Authorization Code + PKCE、Client Credentials、Refresh Token）
- OIDC Discovery（含 `introspection_endpoint`、`end_session_endpoint`、前后端 Channel Logout 声明）
- SAML 2.0 联邦（`spring-security-saml2-service-provider`）
- LDAP/AD 联邦
- MFA（TOTP）
- 密码策略引擎（复杂度、历史密码、有效期）
- **自助密码重置**（邮件可选，开发环境日志输出链接）
- **WebAuthn / Passkey**（无密码登录，支持 Windows Hello / Touch ID / 安全密钥）
- 账号锁定（连续失败锁定）

### 会话与令牌安全
- Redis 分布式会话（Spring Session）
- JDBC 持久化 OAuth2 授权（多实例 HA）
- **JWT 黑名单即时生效**（`RevokedTokenJwtDecoder` 装饰器）
- Token 吊销 / introspection / revocation 端点
- **RP-Initiated Logout（SLO）**：吊销全部 OAuth 授权 + Redis 会话 + Token 黑名单
- 登出跳转白名单可配置（`zest.sso.security.logout-redirect-hosts`）

### 用户与租户治理
- 多租户模型 + **TENANT_ADMIN 行级隔离**（用户列表/管理仅限本租户）
- RBAC：`SSO_ADMIN` / `SSO_OPERATOR` / `TENANT_ADMIN` / `USER`
- SCIM 2.0 用户/组同步
- Vue 3 Admin 控制台

### 可观测性与运维
- Actuator：`/actuator/health`、`/actuator/prometheus`
- 业务指标：`zest.sso.login.*`、`zest.sso.logout`、`zest.sso.token.revoked`、`zest.sso.password.reset.request`
- 审计日志全链路

### 客户端 SDK
- `ZestSsoOidcClient`：PKCE 授权 URL、Claims 解析、**buildLogoutUrl**、Discovery 缓存刷新
- `ZestSsoLogoutTokenValidator`：RP 侧验证 Back-Channel `logout_token`

---

## 规模就绪评估

| 规模 | 就绪度 | 说明 |
|------|--------|------|
| 小企业（<100人） | **可上线** | 单实例 + MySQL + Redis，SSO/MFA/Admin 齐全 |
| 中型企业（100–1000人） | **可上线** | 多实例 HA、SLO、SCIM、租户管理员、Prometheus 指标 |
| 大型企业（1000+人） | **基本可用** | LDAP/SAML/SCIM/多租户 + Back-Channel SLO；建议渗透测试与容灾演练 |

---

## 与 Keycloak / Okta 差距（后续迭代）

| 能力 | 状态 |
|------|------|
| Back-Channel Logout HTTP 回调至各 RP | **已实现**（投递记录 + 重试 + Admin API） |
| Front-Channel Logout iframe 通知 | **已实现** |
| WebAuthn / Passkey | **已实现** |
| JWT 密钥持久化 / 审计归档 / Webhook | **已实现**（M1，见 `docs/full-landing-roadmap.md`） |
| SCIM PATCH / Bulk 完整兼容 | 部分（M2） |
| 等保 / SOC2 合规文档与渗透报告 | 材料包已就绪，待外部机构出报告（见 `docs/compliance/`） |
| 地理容灾 / 多活 | 依赖基础设施 |

---

## 快速验证

```powershell
$env:MYSQL_PASSWORD='123456'
mvn -pl zest-sso-server clean test -Pmysql-it
powershell -File scripts/acceptance.ps1
```

默认账号：`admin` / `admin123`

密码重置（开发）：访问 `/forgot-password`，重置链接打印在服务端日志。

Prometheus：`GET http://localhost:9000/actuator/prometheus`
