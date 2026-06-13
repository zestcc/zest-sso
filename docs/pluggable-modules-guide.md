# 可插拔可选模块指南

ZestSSO 采用 **SPI + Maven 插件 JAR + Admin 在线配置** 模式：未引入 classpath 的 SDK 不会被打包；启用后在 Admin 填写凭据即可生效。

## 模块开关（`zest.sso.modules`）

| 配置键 | 默认 | 说明 |
|--------|------|------|
| `federation` | `true` | 联邦身份源 + `FederatedIdpAdapter` |
| `scim` | `true` | SCIM 2.0 API |
| `webauthn` | `true` | Passkey |
| `webhooks` | `false` | IAM 事件 HTTP 投递队列 |
| `alerts` | `false` | 多通道告警（HTTP/钉钉/企微） |
| `sms-mfa` | `false` | 短信 step-up MFA（需引入短信插件 JAR） |
| `wecom-federation` | `false` | 企微定制换票客户端 |
| `client-onboarding-wizard` | `true` | Admin 应用接入向导 API |

Admin 查询：`GET /api/admin/modules`

Helm 对应：`deploy/helm/zest-sso/values.yaml` → `modules.*` 与环境变量 `ZEST_SSO_MODULES_*`

## SPI 与插件模块

| 能力 | SPI（`zest-sso-plugin-api`） | 内置 / 插件 JAR |
|------|------------------------------|-----------------|
| 联邦 IdP | `federation.spi.FederatedIdpAdapter` | server 内置 |
| MFA 通道 | `plugin.mfa.MfaChannelAdapter` | totp/email 内置；`zest-sso-plugin-aliyun-sms` / `zest-sso-plugin-tencent-sms` 按需引入 |
| 告警通道 | `plugin.alert.AlertChannelAdapter` | http-webhook / dingtalk-bot / wecom-bot 内置 |
| 可选模块元数据 | `modules.spi.OptionalModule` | server 内置 |

### Maven 引入短信插件（默认不打包）

```bash
mvn package -Pwith-sms-plugins
```

或在 `zest-sso-server/pom.xml` 中按需添加依赖：

```xml
<dependency>
  <groupId>cn.zest.sso</groupId>
  <artifactId>zest-sso-plugin-aliyun-sms</artifactId>
</dependency>
```

### Admin 在线配置插件

| API | 说明 |
|-----|------|
| `GET /api/admin/plugins` | 已安装 / 已启用 / 已配置 状态 |
| `PUT /api/admin/plugins/{pluginKey}` | 启用开关 + 凭据 JSON |

数据库表：`sso_plugin_config`（Flyway V16）

## 告警 Admin 在线配置

1. `zest.sso.modules.alerts: true`
2. Admin → **告警通道** 新建配置（优先于 YAML）
3. API：`/api/admin/alert-channels` CRUD

数据库表：`sso_alert_channel`（Flyway V16）

YAML 仍可作为兜底（DB 无记录时生效）：

```yaml
zest:
  sso:
    modules:
      alerts: true
    alerts:
      enabled: true
      channels:
        - channel-key: dingtalk-bot
          enabled: true
          events: [USER_LOGIN_FAILED]
          config:
            webhookUrl: https://oapi.dingtalk.com/robot/send?access_token=xxx
```

## Admin 静态资源打包

| 方式 | 命令 |
|------|------|
| 脚本（推荐） | `scripts/build-admin.ps1` |
| Maven Profile | `mvn package -Pwith-admin-ui`（需 Maven 3.6+ 与 Node） |

产物目录：`zest-sso-server/src/main/resources/static/admin/`，访问 `/admin/`。

## Admin 控制台页面

| 页面 | 路径 |
|------|------|
| 可插拔模块 + SDK 插件配置 | `/modules` |
| 告警通道在线配置 | `/alert-channels` |
| 身份联邦（adapterKey 下拉） | `/identity-providers` |
| 应用接入向导 | `/clients` |
| Webhook 投递记录 | `/webhooks` |

## 扩展新 SDK 插件

1. 新建 Maven 模块，实现 `zest-sso-plugin-api` 中对应接口
2. 添加 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
3. 在 `PluginCatalogService.KNOWN_PLUGINS` 登记元数据（可选）
4. 部署时按需将插件 JAR 加入 server 依赖，**Admin 启用并配置凭据**

无需修改 `MfaService` / `AlertNotificationService` 主流程。

## 说明：zest-llm Back-Channel

OIDC Back-Channel Logout 的 RP 侧实现属于业务应用（如 zest-llm），不在 ZestSSO 仓库内实现。
