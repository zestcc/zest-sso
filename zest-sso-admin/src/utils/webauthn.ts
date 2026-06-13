function bufferFromBase64Url(value: string): ArrayBuffer {
  const padding = '='.repeat((4 - (value.length % 4)) % 4)
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(base64)
  const buffer = new Uint8Array(raw.length)
  for (let i = 0; i < raw.length; i++) buffer[i] = raw.charCodeAt(i)
  return buffer.buffer
}

function encodeBase64Url(buffer: ArrayBuffer): string {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

export function parseAuthenticationOptions(options: Record<string, unknown>) {
  const copy = { ...options }
  if (typeof copy.challenge === 'string') {
    copy.challenge = bufferFromBase64Url(copy.challenge)
  }
  if (Array.isArray(copy.allowCredentials)) {
    copy.allowCredentials = copy.allowCredentials.map((c: Record<string, unknown>) => ({
      ...c,
      id: typeof c.id === 'string' ? bufferFromBase64Url(c.id) : c.id,
    }))
  }
  return copy as PublicKeyCredentialRequestOptions
}

export function parseRegistrationOptions(options: Record<string, unknown>) {
  const copy = { ...options }
  if (typeof copy.challenge === 'string') {
    copy.challenge = bufferFromBase64Url(copy.challenge)
  }
  const user = copy.user as Record<string, unknown> | undefined
  if (user && typeof user.id === 'string') {
    copy.user = { ...user, id: bufferFromBase64Url(user.id) }
  }
  if (Array.isArray(copy.excludeCredentials)) {
    copy.excludeCredentials = copy.excludeCredentials.map((c: Record<string, unknown>) => ({
      ...c,
      id: typeof c.id === 'string' ? bufferFromBase64Url(c.id) : c.id,
    }))
  }
  return copy as PublicKeyCredentialCreationOptions
}

export function credentialToJson(credential: Credential): Record<string, unknown> {
  if (!(credential instanceof PublicKeyCredential)) {
    throw new Error('无效的 WebAuthn 凭据')
  }
  const response = credential.response as AuthenticatorAssertionResponse | AuthenticatorAttestationResponse
  const base: Record<string, unknown> = {
    id: credential.id,
    rawId: encodeBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: encodeBase64Url(response.clientDataJSON),
    },
  }
  if ('authenticatorData' in response) {
    const authResponse = response as AuthenticatorAssertionResponse
    ;(base.response as Record<string, unknown>).authenticatorData = encodeBase64Url(authResponse.authenticatorData)
    ;(base.response as Record<string, unknown>).signature = encodeBase64Url(authResponse.signature)
  }
  if ('attestationObject' in response) {
    const regResponse = response as AuthenticatorAttestationResponse
    ;(base.response as Record<string, unknown>).attestationObject = encodeBase64Url(regResponse.attestationObject)
  }
  return base
}

export function isWebAuthnSupported() {
  return typeof window !== 'undefined' && !!window.PublicKeyCredential
}
