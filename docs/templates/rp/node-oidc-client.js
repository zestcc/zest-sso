/**
 * Node.js OIDC 授权码 + PKCE 最小示例
 * 依赖: npm i openid-client express express-session
 *
 * 生产请使用 zest-sso-client-sdk-node 或 openid-client 完整错误处理。
 */
const express = require('express');
const session = require('express-session');
const { Issuer, generators } = require('openid-client');

const ISSUER = process.env.ZEST_SSO_ISSUER || 'http://localhost:9000';
const CLIENT_ID = process.env.ZEST_SSO_CLIENT_ID || 'my-node-app';
const CLIENT_SECRET = process.env.ZEST_SSO_CLIENT_SECRET || 'change-me';
const REDIRECT_URI = process.env.ZEST_SSO_REDIRECT_URI || 'http://localhost:3000/auth/callback';

async function main() {
  const zestIssuer = await Issuer.discover(`${ISSUER}/api/public/.well-known/openid-configuration`);
  const client = new zestIssuer.Client({
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,
    redirect_uris: [REDIRECT_URI],
    response_types: ['code'],
  });

  const app = express();
  app.use(session({ secret: 'change-me', resave: false, saveUninitialized: false }));

  app.get('/login', (req, res) => {
    const codeVerifier = generators.codeVerifier();
    const codeChallenge = generators.codeChallenge(codeVerifier);
    req.session.codeVerifier = codeVerifier;
    const url = client.authorizationUrl({
      scope: 'openid profile email',
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });
    res.redirect(url);
  });

  app.get('/auth/callback', async (req, res) => {
    const params = client.callbackParams(req);
    const tokenSet = await client.callback(REDIRECT_URI, params, {
      code_verifier: req.session.codeVerifier,
    });
    req.session.tokens = tokenSet;
    res.redirect('/profile');
  });

  app.get('/profile', (req, res) => {
    if (!req.session.tokens) return res.redirect('/login');
    res.json({ claims: req.session.tokens.claims() });
  });

  app.listen(3000, () => console.log('http://localhost:3000/login'));
}

main().catch(console.error);
