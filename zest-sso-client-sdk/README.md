# ZestSSO Client SDK

Spring Boot Starter，供 Zest 生态各产品 Admin 接入 ZestSSO / 通用 OIDC。

## 模块

| 包 | 说明 |
|----|------|
| `cn.zest.sso.client` | Back/Front-Channel Logout、`ZestSsoOidcClient` |
| `cn.zest.sso.client.rp` | **RP 统一 SPI**（推荐）：`RpSsoProvider`、`RpSsoAuthService` |

## RP 接入

1. 配置类实现 `RpSsoProperties`
2. 引入本依赖（自动装配 `RpSsoAutoConfiguration`）
3. 注入 `RpSsoAuthService`，回调后自行签发本地 JWT

```yaml
# 示例：ZestFlow
zestflow:
  sso:
    enabled: true
    provider: zest-sso
    issuer: http://localhost:9000
    client-id: zestflow-admin
    client-secret: change-me
    redirect-uri: http://localhost:5173/login/callback
```

## IdP 插件

IdP 侧 MFA/短信等扩展见 `zest-sso-plugin-api`（与 RP SPI 不同层）。
