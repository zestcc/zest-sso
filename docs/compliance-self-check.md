# 等保 2.0 / 企业 IAM 自查清单（ZestSSO）

> 本清单用于上线前自检，**不构成正式等保测评报告**。正式测评需委托具备资质的测评机构。

## 一、身份鉴别（三级相关要求对照）

| 检查项 | ZestSSO 能力 | 状态 | 备注 |
|--------|-------------|------|------|
| 唯一身份标识 | 用户名 + 用户 ID | ✅ | `sso_user` |
| 口令复杂度 | 密码策略引擎 | ✅ | 长度/大小写/数字/历史密码 |
| 登录失败锁定 | 5 次失败锁定 15 分钟 | ✅ | `LoginAttemptService` |
| 多因素认证 | TOTP MFA | ✅ | Admin + Web 登录 |
| 无密码认证 | WebAuthn / Passkey | ✅ | 可选启用 |
| 会话超时 | Redis Session 30min | ✅ | 可配置 |
| 统一身份源 | LDAP / SAML / OIDC IdP | ✅ | 联邦登录 |

## 二、访问控制

| 检查项 | 能力 | 状态 |
|--------|------|------|
| RBAC | SSO_ADMIN / OPERATOR / TENANT_ADMIN / USER | ✅ |
| 租户隔离 | TENANT_ADMIN 行级过滤 | ✅ |
| OAuth Scope | openid/profile/roles/tenant/scim | ✅ |
| 客户端 PKCE | 可强制 require_pkce | ✅ |
| 管理 API 鉴权 | Spring Security + Session | ✅ |

## 三、安全审计

| 检查项 | 能力 | 状态 |
|--------|------|------|
| 登录/登出审计 | `sso_audit_log` | ✅ |
| 用户/客户端变更审计 | 全量 AuditEventType | ✅ |
| Token 吊销审计 | TOKEN_REVOKE | ✅ |
| Passkey 操作审计 | WEBAUTHN_* 事件 | ✅ |
| 日志保留策略 | — | ⚠️ 需运维配置 MySQL 归档 |

## 四、通信与数据安全

| 检查项 | 能力 | 状态 |
|--------|------|------|
| 传输加密 | HTTPS（生产必须） | ⚠️ 部署层 Nginx/网关 TLS |
| 密码存储 | BCrypt | ✅ |
| JWT 签名 | RS256 | ✅ |
| Token 黑名单 | Redis 即时失效 | ✅ |
| 密钥持久化 | JWT PEM 文件注入 | ⚠️ 生产必须配置 |
| 敏感配置 | 环境变量注入 | ✅ MYSQL_PASSWORD 等 |

## 五、单点登出与联合

| 检查项 | 能力 | 状态 |
|--------|------|------|
| RP-Initiated Logout | `/connect/logout` | ✅ |
| Back-Channel Logout | logout_token POST | ✅ |
| Front-Channel Logout | iframe 通知 | ✅ |
| SCIM 供给 | Users/Groups/Bulk | ✅ |
| RP SDK 校验器 | `ZestSsoLogoutTokenValidator` | ✅ |

## 六、可观测性与运维

| 检查项 | 能力 | 状态 |
|--------|------|------|
| 健康检查 | `/actuator/health` | ✅ |
| Prometheus 指标 | `/actuator/prometheus` | ✅ |
| 业务指标 | login/logout/token/passkey | ✅ |
| 多实例 HA | JDBC OAuth2 + Redis Session | ✅ |
| 备份恢复 | MySQL + Redis 备份 | ⚠️ 运维 SOP |
| 渗透测试 | — | ❌ 需外部评估 |

## 七、上线前必做项（Checklist）

- [ ] 更换所有默认密码（admin、OAuth client secret、MySQL）
- [ ] 配置 RSA JWT 密钥文件并限制文件权限
- [ ] 全站 HTTPS，HSTS
- [ ] `zest.sso.webauthn.origins` 与 `logout-redirect-hosts` 仅含生产域名
- [ ] 启用 SMTP 密码重置（或禁用自助重置）
- [ ] Redis / MySQL 网络隔离与强密码
- [ ] 运行 `scripts/acceptance.ps1` 全绿
- [ ] 各 RP 配置 Back-Channel Logout URI 并完成联调
- [ ] 制定审计日志保留与归档策略（建议 ≥ 180 天）

## 八、参考对标

| 产品 | ZestSSO 覆盖度 |
|------|----------------|
| Keycloak | ~75%（缺 UMA、细粒度策略、部分 SPI） |
| Okta | ~70%（缺 Universal Directory 级别生态） |
| Auth0 | ~72%（缺 Rules/Actions 市场） |

**结论**：满足中小型企业生产 IAM；大型企业需补充正式等保测评、渗透测试与容灾演练。
