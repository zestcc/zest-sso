/**
 * ZestSSO OIDC Client SDK for Node.js (minimal, production-oriented).
 * Usage mirrors Auth0/Okta SPA + confidential client patterns.
 */
const crypto = require('crypto');

class ZestSsoClient {
  constructor(options) {
    this.issuer = options.issuer.replace(/\/$/, '');
    this.clientId = options.clientId;
    this.clientSecret = options.clientSecret;
    this.redirectUri = options.redirectUri;
    this.scopes = options.scopes || 'openid profile email';
    this._metadata = null;
  }

  async discover() {
    if (this._metadata) return this._metadata;
    const res = await fetch(`${this.issuer}/api/public/.well-known/openid-configuration`);
    if (!res.ok) throw new Error(`OIDC discovery failed: ${res.status}`);
    this._metadata = await res.json();
    return this._metadata;
  }

  buildAuthorizationUrl(state, codeChallenge) {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: this.clientId,
      redirect_uri: this.redirectUri,
      scope: this.scopes,
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
    });
    return `${this.issuer}/oauth2/authorize?${params}`;
  }

  static generatePkce() {
    const verifier = crypto.randomBytes(32).toString('base64url');
    const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');
    return { verifier, challenge };
  }

  async exchangeCode(code, codeVerifier) {
    const meta = await this.discover();
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: this.redirectUri,
      client_id: this.clientId,
      code_verifier: codeVerifier,
    });
    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (this.clientSecret) {
      const basic = Buffer.from(`${this.clientId}:${this.clientSecret}`).toString('base64');
      headers.Authorization = `Basic ${basic}`;
    }
    const res = await fetch(meta.token_endpoint, { method: 'POST', headers, body });
    if (!res.ok) throw new Error(`Token exchange failed: ${res.status} ${await res.text()}`);
    return res.json();
  }

  async fetchUserInfo(accessToken) {
    const meta = await this.discover();
    const res = await fetch(meta.userinfo_endpoint, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!res.ok) throw new Error(`UserInfo failed: ${res.status}`);
    return res.json();
  }

  async startDeviceAuthorization() {
    const meta = await this.discover();
    const body = new URLSearchParams({
      client_id: this.clientId,
      scope: this.scopes,
    });
    const res = await fetch(meta.device_authorization_endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });
    if (!res.ok) throw new Error(`Device authorization failed: ${res.status}`);
    return res.json();
  }
}

module.exports = { ZestSsoClient };
