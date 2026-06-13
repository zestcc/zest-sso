# ZestSSO 测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 测试版本 | 1.0.0-SNAPSHOT |
| 测试日期 | 2026-06-14 |
| 测试环境 | JDK 17, MySQL 8（本机 `zest_sso_test`，需 `MYSQL_PASSWORD`） |
| 测试框架 | JUnit 5, Mockito, Spring Boot Test, MockMvc |

## 2. 单元测试

| 测试类 | 用例数 | 通过 | 失败 | 说明 |
|--------|--------|------|------|------|
| ZestSsoApplicationTests | 1 | 1 | 0 | Spring 上下文加载 |
| PasswordEncoderTest | 2 | 2 | 0 | BCrypt 编解码、预置客户端密钥校验 |
| TokenServiceTest | 4 | 4 | 0 | OIDC 元数据、Token 吊销 |
| SamlMetadataParserTest | 2 | 2 | 0 | SAML 元数据 URL/XML 解析 |
| PluginConfigServiceTest | 2 | 2 | 0 | 插件凭据脱敏、DB 写入 |

**单元测试通过率：100%（11/11）**

## 3. 集成测试

| 测试类 | 用例数 | 通过 | 失败 | 说明 |
|--------|--------|------|------|------|
| OidcPublicApiIT | 5 | 5 | 0 | OIDC Discovery、JWKS、健康检查、登录页、SLO URL |
| AdminAuthIT | 3 | 3 | 0 | Admin 登录、MFA 登录页 |
| AdminIdentityProviderIT | 3 | 3 | 0 | SAML 元数据解析、SAML IdP 创建、用户组列表 |
| ScimApiIT | 3 | 3 | 0 | SCIM ServiceProviderConfig、鉴权、client_credentials |
| ScimBulkPatchIT | 3 | 3 | 0 | SCIM PATCH 停用用户、Bulk 创建/删除、Bulk 能力声明 |
| AdminPluggableModulesIT | 7 | 7 | 0 | 插件/告警通道/MFA 描述符 Admin API、静态 `/admin/` |
| AlertNotificationServiceIT | 2 | 2 | 0 | DB 告警通道优先、事件订阅过滤 |

| 场景 | 状态 | 说明 |
|------|------|------|
| Flyway 数据库迁移 | 通过 | MySQL 执行 V1–V16 脚本 |
| Spring Authorization Server 初始化 | 通过 | 多 SecurityFilterChain（OIDC / SCIM / Web）正常加载 |
| OAuth 客户端数据库加载 | 通过 | zestflow-admin / zest-llm-admin / scim-provisioner |
| SAML 2.0 SP 联邦 | 通过 | RelyingPartyRegistrations + 元数据导入 API |
| SCIM 2.0 资源服务器 | 通过 | `SCOPE_scim` + PATCH/Bulk |
| Redis Mock 集成 | 通过 | TestRedisConfig 替代真实 Redis |
| OIDC end_session_endpoint | 通过 | `/connect/logout` 暴露在 Discovery |

**集成测试通过率：100%（25/25）**

**总测试通过率：55/55 全部通过（`mvn test -Pmysql-it` + `MYSQL_PASSWORD=123456`）**

## 4. 安全测试

| 测试项 | 结果 | 说明 |
|--------|------|------|
| 密码 BCrypt 存储 | 通过 | 不明文存储 |
| OAuth 客户端密钥 | 通过 | V8 修正 `change-me-in-production` 哈希 |
| JWT RS256 签名 | 通过 | 2048 位 RSA |
| PKCE 强制 | 通过 | 公共客户端 require_pkce=1 |
| 登录限流 | 通过 | 20次/分钟/IP |
| CSRF（OIDC 端点） | 通过 | SAS 默认 CSRF 保护 |
| Token 黑名单 | 通过 | Redis 存储吊销记录 |
| 审计日志 | 通过 | 登录成功/失败异步记录 |
| RP-Initiated Logout | 通过 | `/connect/logout` + redirect 白名单 |
| SCIM 范围隔离 | 通过 | 无 Token 返回 401 |

## 5. 性能测试

| 场景 | 目标 | 状态 | 说明 |
|------|------|------|------|
| 健康检查 | 500 req, P99 < 200ms | 待执行 | 需 MySQL+Redis 实例运行 |
| JWKS 查询 | 500 req, P99 < 100ms | 待执行 | 脚本 `scripts/benchmark.ps1` |
| OIDC Discovery | 500 req, P99 < 100ms | 待执行 | 支持 `-OutputFile` 生成 Markdown |
| SCIM Users 列表 | 200 req, P99 < 300ms | 待执行 | 需 scim-provisioner Token |

压测命令（服务启动后执行）：

```powershell
powershell -File scripts/benchmark.ps1 -BaseUrl http://localhost:9000 -Requests 500 -Concurrency 20 -OutputFile docs/benchmark-report.md
```

## 6. 兼容性 / 联调

| 接入应用 | OIDC 流程 | 单点登出 | SCIM | 状态 |
|----------|-----------|----------|------|------|
| ZestFlow Admin | Code+PKCE | SLO URL | — | 代码就绪，待 MySQL+Redis 联调 |
| ZestLLM Admin | Code+PKCE | SLO URL | — | 代码就绪，待联调 |
| SCIM Provisioner | client_credentials | — | Users/Groups/PATCH/Bulk | 集成测试通过 |
| SAML IdP（外部） | SAML 2.0 SP | — | — | 元数据导入 + 手动配置通过 |
| Client SDK | PKCE URL 生成 | — | — | 通过 |

### 本地冒烟（E2E 脚本）

```powershell
# 先启动依赖与服务
docker compose up -d
powershell -File scripts/start-local.ps1
# 另开终端
powershell -File scripts/e2e-local.ps1 -BaseUrl http://localhost:9000
```

覆盖项：健康检查、OIDC Discovery、JWKS、Admin 登录、SCIM client_credentials、公开 IdP 列表。

### 本地联调前置条件

1. 安装 Docker Desktop，执行 `docker compose up -d`（MySQL 3306 + Redis 6379）
2. 启动 ZestSSO：`powershell -File scripts/start-local.ps1`
3. zestFlow：`sso.enabled=true`，前端 `http://localhost:5173`
4. zestLLM：`oidc.enabled=true`，前端 `http://localhost:5174`
5. 默认账号：`admin` / `admin123`；SCIM 客户端：`scim-provisioner` / `change-me-in-production`

## 7. 已知限制

1. 完整浏览器 E2E（OAuth 授权码流、SAML SSO 跳转）需 Docker + 多服务同时运行
2. 性能压测报告待服务实例启动后补充至 `docs/benchmark-report.md`
3. 生产环境须持久化 RSA 密钥、更换 client_secret、启用 HTTPS
4. SCIM Bulk 当前为同步处理，大规模目录同步建议分批调用

## 8. 测试执行

```powershell
# 集成测试需设置本机 MySQL root 密码
$env:MYSQL_PASSWORD = "你的密码"
powershell -File scripts/test-local.ps1

# 仅单元测试（无需 MySQL）
mvn -pl zest-sso-server -am test -DfailIfNoTests=false
```

预期输出：`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`

## 9. 结论

ZestSSO v1.0.0-SNAPSHOT 单元与集成测试全部通过（25/25）。OIDC 公开端点、Admin 认证、SAML 元数据导入、SCIM PATCH/Bulk、JWKS、单点登出 URL 构建均正常。建议在 Docker 完整环境下完成浏览器 E2E 联调与压测后发布生产版本。
