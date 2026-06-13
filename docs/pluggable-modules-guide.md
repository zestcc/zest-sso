# 可插拔可选模块指南

ZestSSO 采用 **SPI + 配置开关** 模式：功能默认关闭或按需启用，新增能力只需实现接口并注册 Spring Bean。

## 模块开关（`zest.sso.modules`）

| 配置键 | 默认 | 说明 |
|--------|------|------|
| `federation` | `true` | 联邦身份源 + `FederatedIdpAdapter` |
| `scim` | `true` | SCIM 2.0 API |
| `webauthn` | `true` | Passkey |
| `webhooks` | `false` | IAM 事件 HTTP 投递队列 |
| `alerts` | `false` | 多通道告警（HTTP/钉钉/企微） |
| `sms-mfa` | `false` | 短信 step-up MFA |
| `wecom-federation` | `false` | 企微定制换票客户端 |
| `client-onboarding-wizard` | `true` | Admin 应用接入向导 API |

Admin 查询：`GET /api/admin/modules`

Helm 对应：`deploy/helm/zest-sso/values.yaml` → `modules.*` 与环境变量 `ZEST_SSO_MODULES_*`

## SPI 一览

| SPI | 包路径 | 内置实现 |
|-----|--------|----------|
| 联邦 IdP | `federation.spi.FederatedIdpAdapter` | generic-oidc, feishu, dingtalk, wecom |
| OAuth2 换票 | `federation.oauth.spi.FederatedOAuth2TokenClient` | wecom |
| MFA 通道 | `mfa.spi.MfaChannelAdapter` | totp, email, aliyun-sms, tencent-sms |
| 告警通道 | `alert.spi.AlertChannelAdapter` | http-webhook, dingtalk-bot, wecom-bot |
| 可选模块元数据 | `modules.spi.OptionalModule` | 各内置模块描述 Bean |

## 启用示例

### 短信 MFA（阿里云）

```yaml
zest:
  sso:
    modules:
      sms-mfa: true
    mfa:
      step-up-priority: sms,email
      channels:
        aliyun-sms:
          enabled: true
          access-key-id: ${ALIYUN_AK}
          access-key-secret: ${ALIYUN_SK}
          sign-name: ZestSSO
          template-code: SMS_123456
```

用户 `username` 需为 11 位手机号方可接收短信 OTP。

### 钉钉/企微告警

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
          events: [LOGIN_FAILURE, LOGOUT]
          config:
            webhookUrl: https://oapi.dingtalk.com/robot/send?access_token=xxx
        - channel-key: wecom-bot
          enabled: true
          config:
            webhookUrl: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
```

### 企微联邦登录

```yaml
zest:
  sso:
    modules:
      wecom-federation: true
```

创建身份源时 `adapterKey=wecom`，`clientId`/`clientSecret` 填企微 CorpID/Secret。

## Admin 控制台

| 页面 | 路径 |
|------|------|
| 可插拔模块清单 | `/modules` |
| 身份联邦（adapterKey 下拉） | `/identity-providers` |
| 应用接入向导 | `/clients` →「接入向导」 |
| Webhook 投递 | `/webhooks` |

## 扩展新适配器

1. 实现对应 SPI 接口
2. 添加 `@Component`
3. Registry 自动发现
4. （可选）在 `OptionalModuleBeans` 增加模块描述

无需修改核心 `IdentityProviderService` / `MfaService` / `AlertNotificationService` 主流程。
