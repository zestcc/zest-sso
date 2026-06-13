# 企业微信接入说明

## 现状

企业微信提供的是 **OAuth2 网页授权**，并非完整 OIDC（通常无 `.well-known/openid-configuration`）。  
ZestSSO 当前 OIDC 联邦实现通过 Discovery 自动发现 endpoint，因此 **不能** 像飞书一样仅填 JSON 即完成接入。

## 中小企业推荐路径

### 路径 1：LDAP/AD + ZestSSO 本地账号（最稳）

1. Admin → LDAP 提供方，同步组织架构
2. 用户使用域账号登录 ZestSSO
3. 企业微信仅作 IM，不做登录入口

### 路径 2：企业微信作 IdP（需定制，阶段 B）

预期实现：

```
用户 → 企微扫码 → ZestSSO OAuth2 Bridge → 创建/关联本地用户 → SSO Session
```

需产品侧增加 `WECOM` 类型身份源，硬编码：

- `authorize`: `https://open.weixin.qq.com/connect/oauth2/authorize`
- `token`: `https://qyapi.weixin.qq.com/cgi-bin/gettoken`（企业凭证）+ 用户身份接口

### 路径 3：应用单独接企微，ZestSSO 仍作中心 IdP

业务系统直接接企微 OAuth，拿到 `userid` 后再用 ZestSSO **Token Exchange**（若已启用）换统一令牌——适合「仅一个系统要企微登录」的场景。

## 开放平台配置备忘

| 项 | 值 |
|----|-----|
| 授权链接 | `https://open.weixin.qq.com/connect/oauth2/authorize` |
| 企业 ID | `corpId` |
| 应用 AgentId | 自建应用 ID |
| 回调 | 由桥接服务或阶段 B 模块定义 |

## 跟踪

见 [sme-gap-closure-roadmap.md](../../sme-gap-closure-roadmap.md) 阶段 B。
