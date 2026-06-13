# 国内身份源接入指南（钉钉 / 企业微信 / 飞书）

ZestSSO 作为 **OIDC IdP** 的同时，支持将外部 IdP 配置为**联邦登录源**（登录页展示「使用 xxx 登录」按钮）。  
配置入口：Admin 控制台 → 身份源，或 `POST /api/admin/identity-providers`。

## 插拔式适配器（SPI）

国内 IdP 通过 **`adapterKey`** 选择内置适配器，无需改代码即可切换平台：

| adapterKey | 名称 | 生产就绪 | 说明 |
|------------|------|----------|------|
| `generic-oidc` | 通用 OIDC | ✅ | 任意有 Discovery 的 IdP |
| `feishu` | 飞书 | ✅ | 自动填充飞书 Discovery 与 Claims |
| `dingtalk` | 钉钉 | ✅ | Discovery 或 `endpoint_config` 手动端点 |
| `wecom` | 企业微信 | ⚠️ 预览 | 预置授权端点，完整换票见阶段 B |

查询可用适配器：

```http
GET /api/admin/identity-providers/adapters
```

创建身份源时在 JSON 中指定 `adapterKey`；钉钉/企微无 Discovery 时可传 `endpointConfig`：

```json
{
  "endpointConfig": {
    "authorizationUri": "https://login.dingtalk.com/oauth2/auth",
    "tokenUri": "https://api.dingtalk.com/v1.0/oauth2/userAccessToken"
  }
}
```

新增平台适配器：实现 `FederatedIdpAdapter` 并注册为 Spring `@Component`，`FederatedIdpAdapterRegistry` 会自动发现。

## 能力矩阵

| 平台 | 推荐方式 | ZestSSO 现状 | 说明 |
|------|----------|--------------|------|
| **飞书** | OIDC 联邦 | ✅ 推荐 | `adapterKey=feishu`，自动 Discovery |
| **钉钉** | OIDC 联邦（有 Discovery 时） | ✅ `adapterKey=dingtalk` | 无 Discovery 时用 `endpoint_config` |
| **企业微信** | LDAP 同步 + 本地登录 / SAML | ⚠️ `adapterKey=wecom` 预览 | 企微 OAuth2 非标准 OIDC，完整换票待阶段 B |
| **AD/LDAP** | LDAP 联邦 | ✅ 已支持 | Admin → LDAP 提供方 |

## 一、飞书（推荐，步骤最少）

### 1. 飞书开放平台准备

1. 登录 [飞书开放平台](https://open.feishu.cn/) 创建企业自建应用
2. 开启「网页应用」或「登录」能力，配置重定向 URL：
   ```
   https://<你的-sso-域名>/login/oauth2/code/feishu
   ```
3. 记录 **App ID**、**App Secret**
4. 确认 OIDC Discovery 可访问（飞书租户通常支持）：
   ```
   https://open.feishu.cn/.well-known/openid-configuration
   ```

### 2. 使用模板注册

```powershell
# 替换凭据后执行
powershell -File scripts/register-idp-template.ps1 `
  -BaseUrl https://sso.example.com `
  -Template feishu `
  -ClientId "cli_xxxxxxxx" `
  -ClientSecret "xxxxxxxx"
```

或参考模板文件手动 POST：`docs/templates/idp/feishu-oidc.json`

### 3. Claims 映射建议

| 字段 | 建议 claim |
|------|------------|
| 用户名 | `sub` 或 `open_id` |
| 邮箱 | `email` |
| 显示名 | `name` |
| 默认角色 | `USER` |

## 二、钉钉

### 1. 钉钉开放平台

1. [钉钉开放平台](https://open.dingtalk.com/) 创建应用
2. 配置回调域名与 OAuth 回调：
   ```
   https://<sso-域名>/login/oauth2/code/dingtalk
   ```
3. 若控制台提供 **OIDC Discovery URL**，按 OIDC 身份源录入；模板见 `docs/templates/idp/dingtalk-oidc.json.example`

### 2. Admin API 示例

```json
{
  "alias": "dingtalk",
  "displayName": "钉钉登录",
  "providerType": "OIDC",
  "adapterKey": "dingtalk",
  "discoveryUri": "https://login.dingtalk.com/oauth2/.well-known/openid-configuration",
  "clientId": "<AppKey>",
  "clientSecret": "<AppSecret>",
  "scopes": "openid,profile",
  "usernameClaim": "sub",
  "emailClaim": "email",
  "displayNameClaim": "name",
  "defaultRoleCodes": "USER"
}
```

> **注意**：钉钉各版本 Discovery 地址可能不同；若 Discovery 不可用，请改用 LDAP 同步钉钉通讯录，或等待阶段 B「手动 endpoint」支持。

## 三、企业微信

企业微信当前 **无标准 OIDC Discovery**，ZestSSO 的 `DatabaseClientRegistrationRepository` 依赖 Discovery 拉取 endpoint。

### 推荐替代方案（中小企业常用）

| 方案 | 适用 |
|------|------|
| **A. LDAP/AD** | 已有域控，企微仅作 IM |
| **B. 企微扫码 + 自建桥接** | 需开发轻量 OAuth2 桥接服务（阶段 B 产品化） |
| **C. SAML** | 若企微侧提供 SAML IdP（部分私有化部署） |

详细字段说明：`docs/templates/idp/wecom-oauth-notes.md`

## 四、验证联邦登录

1. Admin 启用身份源
2. 访问 `https://<sso>/login`，应出现对应按钮
3. 完成授权后检查：
   - 用户是否自动入库（`sso_user` + `sso_provider_link`）
   - 审计日志 `FEDERATED_LOGIN` 事件
4. 运行 `scripts/acceptance.ps1` 确保核心 OIDC 未回归

## 五、常见问题

| 现象 | 处理 |
|------|------|
| Discovery 加载失败 | 检查服务器出网、URL 是否正确、IdP 是否启用 OIDC |
| 回调 404 | 确认 `redirect_uri` 为 `{baseUrl}/login/oauth2/code/{alias}` |
| 用户无邮箱 | 调整 `usernameClaim` 为平台实际返回字段 |
| 按钮不显示 | 身份源 `enabled=1` 且 `listEnabledPublic()` 有记录 |

## 六、后续（阶段 B）

- 企微 **自定义 TokenResponseClient**（非标准换票）
- Admin 向导：从 `/adapters` 选择平台 → 自动填充 Discovery / 回调 URL
- 扫码登录 UI 组件
