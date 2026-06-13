# Vue 3 SPA 接入要点

## 1. 环境变量（.env）

```
VITE_SSO_ISSUER=https://sso.example.com
VITE_SSO_CLIENT_ID=my-vue-app
VITE_SSO_REDIRECT_URI=https://app.example.com/login/callback
```

公网 SPA 必须使用 **PKCE**，`client_secret` 不得写入前端。

## 2. 登录跳转（生成 PKCE）

```typescript
import { createPkcePair } from './pkce'; // 或 @zest/sso-client-sdk-node 等价逻辑

export async function startSsoLogin() {
  const { verifier, challenge } = await createPkcePair();
  sessionStorage.setItem('pkce_verifier', verifier);
  const state = crypto.randomUUID();
  sessionStorage.setItem('oauth_state', state);

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: import.meta.env.VITE_SSO_CLIENT_ID,
    redirect_uri: import.meta.env.VITE_SSO_REDIRECT_URI,
    scope: 'openid profile email',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  });
  window.location.href = `${import.meta.env.VITE_SSO_ISSUER}/oauth2/authorize?${params}`;
}
```

## 3. 回调页（`/login/callback`）

**推荐**：将 `code` 发给自家 BFF，由后端换票：

```typescript
// LoginCallback.vue onMounted
const code = new URLSearchParams(location.search).get('code');
const verifier = sessionStorage.getItem('pkce_verifier');
const res = await fetch('/api/auth/sso/callback', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ code, codeVerifier: verifier }),
  credentials: 'include',
});
if (res.ok) router.replace('/dashboard');
```

## 4. Admin 客户端注册

- `redirectUris`: `https://app.example.com/login/callback`
- `requirePkce`: `true`
- 授权类型: `authorization_code`, `refresh_token`

## 5. 登出

```typescript
// 先清本地 token，再跳转 SSO RP-Initiated Logout
window.location.href = `${issuer}/connect/logout?post_logout_redirect_uri=${encodeURIComponent(appOrigin)}`;
```

Back-Channel 需 BFF 实现 logout_token 接收端，参见 Cookbook Step 4。
