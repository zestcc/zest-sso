# ZestSSO 集成指南

本文档说明如何将 ZestFlow 和 ZestLLM 接入 ZestSSO。

## 1. 集成模式

推荐采用 **OIDC 桥接模式**：

```
用户 → ZestSSO 认证 → 获取 id_token → 接入应用换发本地 JWT → 现有安全框架不变
```

优点：最小侵入，保留各应用自有 RBAC 和租户逻辑。

## 2. ZestFlow Admin 集成

### 2.1 ZestSSO 侧配置

预注册客户端 `zestflow-admin`（已内置）：

| 配置项 | 值 |
|--------|-----|
| client_id | zestflow-admin |
| client_secret | change-me-in-production |
| redirect_uri | http://localhost:5173/login/callback |
| grant_types | authorization_code, refresh_token |
| scopes | openid, profile, email, roles, tenant |
| require_pkce | true |

### 2.2 ZestFlow 后端配置

`application.yml` 新增：

```yaml
zestflow:
  sso:
    enabled: true
    issuer: http://localhost:9000
    client-id: zestflow-admin
    client-secret: change-me-in-production
    redirect-uri: http://localhost:8080/api/zestflow/auth/sso/callback
    jwks-uri: http://localhost:9000/oauth2/jwks
```

需新增接口：
- `GET /api/zestflow/auth/sso/authorize` — 生成 PKCE 授权 URL
- `POST /api/zestflow/auth/sso/callback` — 用 code 换 token，映射本地用户，签发 ZestFlow JWT

### 2.3 ZestFlow 前端配置

```typescript
// 登录页增加 SSO 按钮
const handleSsoLogin = async () => {
  const { authorizationUrl } = await api.get('/auth/sso/authorize')
  window.location.href = authorizationUrl
}

// 回调页 /login/callback
const code = new URLSearchParams(location.search).get('code')
const { token, userInfo } = await api.post('/auth/sso/callback', { code })
localStorage.setItem('token', token)
```

### 2.4 角色映射

| ZestSSO roles | ZestFlow 映射 |
|---------------|---------------|
| SSO_ADMIN | is_super_admin = 1 |
| USER | 默认 APP_VIEWER |

### 2.5 用户表扩展

```sql
ALTER TABLE user ADD COLUMN sso_subject VARCHAR(128) DEFAULT NULL;
ALTER TABLE user ADD COLUMN sso_provider VARCHAR(32) DEFAULT 'zest-sso';
ALTER TABLE user ADD UNIQUE KEY uk_sso_subject (sso_provider, sso_subject);
```

## 3. ZestLLM Admin 集成

### 3.1 ZestSSO 侧配置

预注册客户端 `zest-llm-admin`（已内置）：

| 配置项 | 值 |
|--------|-----|
| client_id | zest-llm-admin |
| redirect_uri | http://localhost:5174/login/callback |
| scopes | openid, profile, email, roles, tenant |

### 3.2 ZestLLM 后端配置

复用已有 OIDC 换票 API，修改 issuer 指向 ZestSSO：

```yaml
zest-llm:
  admin:
    oidc:
      enabled: true
      issuer: http://localhost:9000
      audience: zest-llm-admin
      jwks-uri: http://localhost:9000/oauth2/jwks
      client-id: zest-llm-admin
      username-claim: preferred_username
      default-role: ADMIN
```

### 3.3 ZestLLM 前端配置

```typescript
// LoginView.vue
const handleSsoLogin = () => {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: 'zest-llm-admin',
    redirect_uri: 'http://localhost:5174/login/callback',
    scope: 'openid profile email roles tenant',
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
    state: state,
  })
  window.location.href = `http://localhost:9000/oauth2/authorize?${params}`
}

// 回调后调用已有 API
const idToken = await exchangeCodeForIdToken(code)
const { token } = await api.post('/api/admin/auth/oidc/exchange', { idToken })
```

### 3.4 Runtime API 集成

在 App Auth Binding 中配置：

```json
{
  "mode": "OIDC_JWT",
  "issuer": "http://localhost:9000",
  "audience": "zest-llm",
  "jwksUri": "http://localhost:9000/oauth2/jwks"
}
```

## 4. Client SDK 使用

### 4.1 添加依赖

```xml
<dependency>
    <groupId>cn.zest.sso</groupId>
    <artifactId>zest-sso-client-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 4.2 配置

```yaml
zest:
  sso:
    client:
      enabled: true
      issuer: http://localhost:9000
      client-id: your-app
      client-secret: your-secret
      redirect-uri: http://localhost:8080/login/callback
```

### 4.3 代码示例

```java
@Autowired
private ZestSsoOidcClient oidcClient;

// 生成授权 URL
AuthorizationRequest request = oidcClient.buildAuthorizationUrl();
// 前端跳转到 request.getAuthorizationUrl()
// 保存 request.getState() 和 request.getCodeVerifier() 到 session

// 解析 JWT Claims
SsoUserPrincipal principal = oidcClient.parsePrincipal(claims);
```

### 4.4 Device Authorization Grant（RFC 8628）

适用于 CLI、智能电视等无法安全保存 `client_secret` 的公网客户端。预置演示客户端 `device-cli`（`client_authentication_methods=none`）。

```bash
# 1. 获取 device_code / user_code
curl -X POST http://localhost:9000/oauth2/device_authorization \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=device-cli&scope=openid profile"

# 2. 用户在浏览器打开 verification_uri_complete 完成授权

# 3. 设备轮询 token（公网客户端仅传 client_id）
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code={device_code}&client_id=device-cli"
```

Node SDK：`ZestSsoClient.startDeviceAuthorization()`。验收：`scripts/acceptance-m2-m4.ps1` 含 POST 黑盒检查。

## 5. 单点登出（SLO）

ZestSSO 提供 RP-Initiated Logout 端点：

```
GET /connect/logout?post_logout_redirect_uri={url}&id_token_hint={id_token}
```

登出时会：
1. 向各 RP 的 **Back-Channel Logout URI** 异步 POST `logout_token`（OIDC Back-Channel Logout 1.0）
2. 通过隐藏 iframe 触发 **Front-Channel Logout URI**（如已配置）
3. 吊销全部 OAuth 授权、Redis 会话与 Token 黑名单

在 Admin「应用接入」中配置：
- `backchannelLogoutUri` — RP 接收 `logout_token` 的端点
- `frontchannelLogoutUri` — RP 前端登出页（可选）

RP 需验证 `logout_token` JWT（`aud` = client_id，`events` 含 backchannel-logout）。SDK 提供 `ZestSsoLogoutTokenValidator` 开箱即用。

**ZestFlow / ZestLLM 接入**：见 [zestflow-backchannel-integration.md](zestflow-backchannel-integration.md)（ZestLLM 可参考同目录 `zest-llm-admin` 实现）。

## 6. Passkey（WebAuthn）接入

1. 用户在「个人中心」或 SSO 登录页注册 Passkey
2. 登录时点击「使用 Passkey 登录」（可填用户名缩小范围，或留空走 Resident Key）
3. 配置 `zest.sso.webauthn.rp-id` 与 `origins` 白名单（生产域名须一致）

## 7. 联调验证

```bash
# 1. 检查 Discovery
curl http://localhost:9000/api/public/.well-known/openid-configuration
```

### ZestFlow SLO

- 后端：`GET /auth/sso/logout-url` 返回 ZestSSO 登出 URL
- 前端：SSO 登录成功后设置 `sso_login=true`；退出时跳转 ZestSSO SLO，再重定向回登录页
- 配置：`zestflow.sso.post-logout-redirect-uri`（默认 `http://localhost:5173/login`）

### ZestLLM SLO

- 后端：`GET /api/admin/auth/oidc/logout-url`
- 前端：SSO 回调后设置 `zest-llm-sso-login=true`；退出时同上
- 配置：`zest-llm.admin.oidc.post-logout-redirect-uri`（默认 `http://localhost:5174/login`）

### 白名单

`post_logout_redirect_uri` 须为 `localhost` / `127.0.0.1` 或 `*.zest.wang` / `*.zestflow.cn` 域名。

## 8. 新应用接入清单

- [ ] 在 ZestSSO Admin 注册 OAuth 客户端
- [ ] 配置 redirect_uri（精确匹配）
- [ ] 前端实现 PKCE 授权流程
- [ ] 后端验证 id_token（JWKS）
- [ ] 映射用户到本地用户表
- [ ] 映射角色到本地 RBAC
- [ ] 映射租户（如有多租户）
- [ ] 配置 CORS（ZestSSO 侧）
- [ ] 生产环境更换 client_secret
- [ ] 实现单点登出（调用 `/connect/logout`）
- [ ] 启用 HTTPS

## 7. 联调验证

```bash
# 1. 检查 Discovery
curl http://localhost:9000/api/public/.well-known/openid-configuration

# 2. 检查 JWKS
curl http://localhost:9000/oauth2/jwks

# 3. 浏览器访问授权（需先登录 SSO）
http://localhost:9000/oauth2/authorize?response_type=code&client_id=zestflow-admin&redirect_uri=http://localhost:5173/login/callback&scope=openid%20profile&code_challenge=xxx&code_challenge_method=S256
```
