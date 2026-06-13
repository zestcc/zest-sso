# ZestSSO

Zest 生态统一单点登录系统，基于 OAuth 2.0 / OpenID Connect 标准协议，为 ZestFlow、ZestLLM 及未来应用提供集中式身份认证与授权服务。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Spring Boot | 3.2.5 |
| Spring Authorization Server | 1.2.x |
| MyBatis-Plus | 3.5.15 |
| MySQL | 8.x |
| Redis | 7.x |
| Flyway | 10.x |

## 模块结构

```
zest-sso/
├── zest-sso-common/      # 公共常量、异常、API 封装
├── zest-sso-server/      # OIDC 授权服务器 + 管理 API
├── zest-sso-client-sdk/  # 接入应用 Spring Boot Starter
├── zest-sso-admin/       # 管理控制台（Vue 3 SPA）
└── docs/                 # 设计/部署/集成文档
```

## 快速启动

### 1. 启动依赖服务

```bash
docker compose up -d
```

### 2. 配置数据库

创建数据库 `zest_sso`，修改 `application.yml` 中的数据源配置。

### 3. 启动服务

```bash
mvn -pl zest-sso-server spring-boot:run
```

服务默认监听 `http://localhost:9000`

### 4. 启动管理控制台

```bash
cd zest-sso-admin
npm install
npm run dev
```

管理台地址：
- 开发：`http://localhost:5175`
- 生产（同域）：`http://localhost:9000/admin/`

构建生产 Admin：`powershell -File scripts/build-admin.ps1`

### 5. 默认账号

| 账号 | 密码 | 角色 | 权限 |
|------|------|------|------|
| admin | admin123 | SSO_ADMIN | 全部管理功能 |
| operator | operator123 | SSO_OPERATOR | 用户管理、审计查看 |

## OIDC 端点

| 端点 | 说明 |
|------|------|
| `/.well-known/openid-configuration` | OIDC Discovery |
| `/oauth2/authorize` | 授权端点 |
| `/oauth2/token` | Token 端点 |
| `/oauth2/jwks` | JWKS 公钥 |
| `/userinfo` | 用户信息 |
| `/oauth2/revoke` | Token 吊销 |

## 预注册客户端

| Client ID | 应用 | 默认密钥 |
|-----------|------|----------|
| zestflow-admin | ZestFlow Admin | change-me-in-production |
| zest-llm-admin | ZestLLM Admin | change-me-in-production |

## 文档

- [架构设计](docs/architecture.md)
- [部署手册](docs/deployment.md)（含 Helm / 备份 / 监控）
- [集成指南](docs/integration-guide.md)
- [中小企业差距闭环](docs/sme-gap-closure-roadmap.md)
- [RP 集成 Cookbook](docs/rp-integration-cookbook.md)
- [国内 IdP（飞书/钉钉/企微）](docs/domestic-idp-guide.md)
- [合规材料包](docs/compliance/README.md)
- [用户手册](docs/user-manual.md)

## 构建

```bash
mvn clean package
```

## License

Copyright © Zest Team
