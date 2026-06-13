# ZestSSO 用户手册

## 1. 系统登录

### 1.1 SSO 管理控制台（推荐）

1. 启动后端后，在 `zest-sso-admin` 目录执行 `npm install && npm run dev`
2. 浏览器访问 `http://localhost:5175`
3. 使用管理员账号登录，进入完整管理界面

功能包括：应用接入管理、用户管理、租户查看、审计日志、系统概览。

### 1.2 SSO 内置登录页

1. 浏览器访问 `http://localhost:9000/login`
2. 输入用户名和密码
3. 登录成功后进入简易控制台首页

默认管理员账号：

| 字段 | 值 |
|------|-----|
| 用户名 | admin |
| 密码 | admin123 |

> 首次登录后请立即修改密码。

### 1.3 应用 SSO 登录

当 ZestFlow / ZestLLM 等应用启用 SSO 后：

1. 在应用登录页点击「ZestSSO 登录」
2. 跳转到 ZestSSO 统一登录页
3. 输入凭据认证
4. 自动跳转回应用并完成登录

## 2. 管理员操作

### 2.1 用户管理

**创建用户**

```bash
curl -X POST http://localhost:9000/api/admin/users \
  -H "Content-Type: application/json" \
  -b "JSESSIONID=xxx" \
  -d '{
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "password": "SecurePass123",
    "displayName": "张三",
    "roleCodes": ["USER"],
    "tenantIds": [1],
    "defaultTenantId": 1
  }'
```

**查询用户列表**

```bash
curl "http://localhost:9000/api/admin/users?page=1&size=20" -b "JSESSIONID=xxx"
```

### 2.2 客户端管理

**注册新应用**

```bash
curl -X POST http://localhost:9000/api/admin/clients \
  -H "Content-Type: application/json" \
  -b "JSESSIONID=xxx" \
  -d '{
    "clientId": "my-new-app",
    "clientSecret": "my-secret-key-123",
    "clientName": "我的新应用",
    "authorizationGrantTypes": ["authorization_code", "refresh_token"],
    "redirectUris": ["http://localhost:3000/callback"],
    "scopes": ["openid", "profile", "email", "roles", "tenant"],
    "requirePkce": true
  }'
```

### 2.3 审计日志

```bash
curl "http://localhost:9000/api/admin/audit-logs?eventType=LOGIN_FAILURE&page=1&size=20" \
  -b "JSESSIONID=xxx"
```

## 3. API 文档

启动服务后访问 Swagger UI：

```
http://localhost:9000/swagger-ui.html
```

## 4. 常见问题

### Q: 登录提示「请求过于频繁」？

A: 同一 IP 每分钟最多 20 次登录尝试。等待 60 秒后重试。

### Q: OIDC 回调报 redirect_uri 不匹配？

A: 确保应用配置的回调地址与 ZestSSO 客户端注册的 redirect_uri 完全一致（包括协议、端口、路径）。

### Q: 如何启用 HTTPS？

A: 生产环境通过 Nginx/ALB 终结 TLS，并设置 `zest.sso.issuer` 为 HTTPS 地址。

### Q: 如何实现单点登出？

A: 应用登出时清除本地 Token，并引导用户访问 ZestSSO `/logout` 端点销毁 SSO Session。

## 5. 角色说明

| 角色 | 能力 |
|------|------|
| SSO_ADMIN | 管理用户、客户端、租户、查看审计 |
| SSO_OPERATOR | 管理普通用户、查看审计 |
| USER | 仅用于 SSO 认证，无管理权限 |

## 6. 安全建议

1. 生产环境立即修改默认管理员密码
2. 更换所有预注册客户端的 client_secret
3. 配置持久化 RSA 密钥（非运行时生成）
4. 启用 HTTPS
5. 定期审查审计日志
6. 限制 Admin API 访问来源 IP
