"""ZestSSO OIDC Client SDK for Python (minimal)."""

from __future__ import annotations

import base64
import hashlib
import secrets
from typing import Any
from urllib.parse import urlencode

import urllib.request


class ZestSsoClient:
    def __init__(
        self,
        issuer: str,
        client_id: str,
        redirect_uri: str,
        client_secret: str | None = None,
        scopes: str = "openid profile email",
    ) -> None:
        self.issuer = issuer.rstrip("/")
        self.client_id = client_id
        self.client_secret = client_secret
        self.redirect_uri = redirect_uri
        self.scopes = scopes
        self._metadata: dict[str, Any] | None = None

    def discover(self) -> dict[str, Any]:
        if self._metadata:
            return self._metadata
        with urllib.request.urlopen(
            f"{self.issuer}/api/public/.well-known/openid-configuration"
        ) as resp:
            self._metadata = json_load(resp.read())
        return self._metadata

    @staticmethod
    def generate_pkce() -> tuple[str, str]:
        verifier = secrets.token_urlsafe(32)
        challenge = base64.urlsafe_b64encode(
            hashlib.sha256(verifier.encode()).digest()
        ).rstrip(b"=").decode()
        return verifier, challenge

    def build_authorization_url(self, state: str, code_challenge: str) -> str:
        params = urlencode(
            {
                "response_type": "code",
                "client_id": self.client_id,
                "redirect_uri": self.redirect_uri,
                "scope": self.scopes,
                "state": state,
                "code_challenge": code_challenge,
                "code_challenge_method": "S256",
            }
        )
        return f"{self.issuer}/oauth2/authorize?{params}"

    def exchange_code(self, code: str, code_verifier: str) -> dict[str, Any]:
        import json

        meta = self.discover()
        body = urlencode(
            {
                "grant_type": "authorization_code",
                "code": code,
                "redirect_uri": self.redirect_uri,
                "client_id": self.client_id,
                "code_verifier": code_verifier,
            }
        ).encode()
        req = urllib.request.Request(
            meta["token_endpoint"],
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        if self.client_secret:
            token = base64.b64encode(
                f"{self.client_id}:{self.client_secret}".encode()
            ).decode()
            req.add_header("Authorization", f"Basic {token}")
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode())

    def fetch_userinfo(self, access_token: str) -> dict[str, Any]:
        import json

        meta = self.discover()
        req = urllib.request.Request(
            meta["userinfo_endpoint"],
            headers={"Authorization": f"Bearer {access_token}"},
        )
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode())


def json_load(raw: bytes) -> dict[str, Any]:
    import json

    return json.loads(raw.decode())
