# ZestSSO 生产级验收报告

| 项目 | 内容 |
|------|------|
| 版本 | 1.0.0 |
| 开始时间 | 2026-06-14 19:20:20 |
| 结束时间 | 2026-06-14 19:22:49 |
| 耗时 | 2.5 分钟 |
| 环境 | JDK 17, MySQL 8, Redis 7, Windows 本地 |
| 服务地址 | http://localhost:9000 |
| **验收结论** | **通过 (PASS)** |

## 1. 白盒测试（White-box）

代码级单元测试与 Spring Boot 集成测试，覆盖安全、OIDC、SCIM、SAML、Admin 等核心模块。

| 类别 | 状态 | 用例数 | 失败 | 错误 |
|------|------|--------|------|------|
| 单元测试 (mvn test) | PASS | 3 | 0 | 0 |
| 集成测试 (mvn test -Pmysql-it) | PASS | 5 | 0 | 0 |

集成测试含：AdminSessionChainIT、OidcPublicApiIT、ScimApiIT、ScimBulkPatchIT、AdminAuthIT、AdminIdentityProviderIT、WebAuthnPublicApiIT。

## 2. 黑盒测试（Black-box）

对运行中服务进行外部 API 冒烟，不依赖内部实现。

| 项目 | 状态 | 通过项 | 失败项 |
|------|------|--------|--------|
| E2E 冒烟 (e2e-local.ps1) | PASS | 10 | 0 |

覆盖：健康检查、OIDC Discovery、JWKS、SCIM 配置、Admin 登录+会话、SCIM Token、登出 URL、登录页、WebAuthn 登录选项。

## 3. 链路测试（Chain / E2E Flow）

| 链路 | 状态 | 步骤数 |
|------|------|--------|
| 全链路 (chain-local.ps1) | PASS | 22 |

| 链路名称 | 验证步骤 |
|----------|----------|
| OIDC-Public | Discovery → JWKS → Client Credentials Token |
| Admin-Session | Login → /me → Clients → Users → Dashboard → Logout → 401 |
| SCIM-Lifecycle | Token → Config → Create User → PATCH 停用 → 验证 → Delete |
| Security | 错误密码拒绝、无 Token SCIM 401 |
| OIDC-Authorize | PKCE 授权请求重定向至登录 |
| WebAuthn-SLO | WebAuthn 登录选项、RP logout URI 配置、Discovery backchannel 声明 |

## 4. 压力测试（Stress）

| 指标 | 阈值 | 实测 | 状态 |
|------|------|------|------|
| 错误率 | 0% | 0 errors | PASS |
| Health P99 | < 500ms | 255.37 ms | PASS |
| 并发 | 20 | 500 req/endpoint | — |

详细压测数据见 [benchmark-report.md](benchmark-report.md)。

## 5. 生产准入检查清单

| 检查项 | 状态 | 说明 |
|--------|------|------|
| MySQL 持久化（禁用 H2） | PASS | MysqlOnlyDataSourceGuard 强制 MySQL |
| Redis Session | PASS | Spring Session Redis |
| OAuth2/OIDC 标准端点 | PASS | SAS 官方实现 |
| Admin 会话认证 | PASS | 登录后 Session 持久化 |
| SCIM 2.0 PATCH/Bulk | PASS | 链路测试验证 |
| SAML 元数据导入 | PASS | 集成测试验证 |
| 登录限流 | PASS | 20次/分钟/IP |
| JWT RS256 | PASS | 2048-bit RSA |
| Flyway 迁移 V1-V8 | PASS | 自动执行 |
| HTTPS / 持久化密钥 | 待生产配置 | 部署时配置 application-prod.yml |

## 6. 执行命令

    $env:MYSQL_PASSWORD = '123456'
    powershell -File scripts/acceptance.ps1

## 7. 结论

PASS: ZestSSO v1.0.0 production acceptance completed.
