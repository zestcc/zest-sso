# ZestFlow Back-Channel Logout 接入指南

> ZestFlow 代码库不在本 monorepo 时，按本文在 **ZestFlow Admin 后端** 集成即可（与 ZestLLM 方案一致）。

## 1. Maven 依赖

```xml
<dependency>
    <groupId>cn.zest.sso</groupId>
    <artifactId>zest-sso-client-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

本地安装 ZestSSO：

```bash
cd D:\project\zest\zest-sso
mvn install -DskipTests
```

## 2. 实现 SsoLogoutHandler

```java
@Component
@ConditionalOnProperty(prefix = "zestflow.sso", name = "enabled", havingValue = "true")
public class ZestFlowSsoLogoutHandler implements SsoLogoutHandler {

    private final SessionRevocationService sessionRevocationService;

    @Override
    public void onBackchannelLogout(String principal) {
        sessionRevocationService.revokeByUsername(principal);
    }
}
```

`SessionRevocationService`：将用户名写入 Redis（TTL ≥ JWT 有效期），在 JWT 过滤器中拒绝已吊销用户。

## 3. application.yml

```yaml
zestflow:
  sso:
    enabled: true
    client-id: zestflow-admin
    issuer: http://localhost:9000

zest:
  sso:
    client:
      enabled: ${zestflow.sso.enabled:false}
      issuer: ${zestflow.sso.issuer:http://localhost:9000}
      client-id: ${zestflow.sso.client-id:zestflow-admin}
      backchannel-logout-path: /api/zestflow/auth/sso/backchannel-logout
      frontchannel-logout-path: /auth/frontchannel-logout
```

## 4. Spring Security 放行

```java
.requestMatchers("/api/zestflow/auth/sso/backchannel-logout").permitAll()
.requestMatchers("/auth/frontchannel-logout").permitAll()
```

## 5. ZestSSO Admin 配置

在「应用接入」编辑 `zestflow-admin`（或由 Flyway `V12` 自动写入）：

| 字段 | 示例值 |
|------|--------|
| backchannelLogoutUri | `http://localhost:8080/api/zestflow/auth/sso/backchannel-logout` |
| frontchannelLogoutUri | `http://localhost:5173/auth/frontchannel-logout` |

## 6. 验证

```bash
# 1. 用户在 ZestFlow 通过 SSO 登录
# 2. 在 ZestSSO 执行全局登出
curl "http://localhost:9000/connect/logout?id_token_hint=<id_token>&post_logout_redirect_uri=http://localhost:5173/login"

# 3. 确认 ZestFlow 后续 API 请求 JWT 被拒绝（401）
```

## 7. 参考实现

完整可参考 **ZestLLM Admin**：

- `AdminSessionRevocationService`
- `ZestLlmSsoLogoutHandler`
- `JwtAuthFilter` 吊销检查

路径：`D:\project\zest\zest-llm\zest-llm-admin\src\main\java\cn\zest\www\zestllm\admin\service\sso\`
