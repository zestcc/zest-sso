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

function challengeToBuffer(challenge: unknown): ArrayBuffer | unknown {
  if (typeof challenge === 'string') {
    return bufferFromBase64Url(challenge)
  }
  if (challenge && typeof challenge === 'object' && 'value' in challenge) {
    const value = (challenge as { value?: unknown }).value
    if (typeof value === 'string') {
      return bufferFromBase64Url(value)
    }
  }
  return challenge
}

function credentialIdToBuffer(id: unknown): ArrayBuffer | unknown {
  if (typeof id === 'string') {
    return bufferFromBase64Url(id)
  }
  if (Array.isArray(id)) {
    return new Uint8Array(id as number[]).buffer
  }
  return id
}

function sanitizePublicKeyOptions<T extends Record<string, unknown>>(options: T): T {
  const copy = { ...options }
  copy.challenge = challengeToBuffer(copy.challenge)
  if (copy.allowCredentials === null || copy.allowCredentials === undefined) {
    delete copy.allowCredentials
  } else if (Array.isArray(copy.allowCredentials)) {
    copy.allowCredentials = copy.allowCredentials.map((c: Record<string, unknown>) => ({
      ...c,
      id: credentialIdToBuffer(c.id),
    }))
  }
  if (copy.excludeCredentials === null || copy.excludeCredentials === undefined) {
    delete copy.excludeCredentials
  } else if (Array.isArray(copy.excludeCredentials)) {
    copy.excludeCredentials = copy.excludeCredentials.map((c: Record<string, unknown>) => ({
      ...c,
      id: credentialIdToBuffer(c.id),
    }))
  }
  if (copy.extensions === null) {
    delete copy.extensions
  }
  return copy
}

export function parseAuthenticationOptions(options: Record<string, unknown>) {
  return sanitizePublicKeyOptions(options) as unknown as PublicKeyCredentialRequestOptions
}

export function parseRegistrationOptions(options: Record<string, unknown>) {
  const copy = sanitizePublicKeyOptions(options)
  const user = copy.user as Record<string, unknown> | undefined
  if (user) {
    copy.user = { ...user, id: credentialIdToBuffer(user.id) }
  }
  return copy as unknown as PublicKeyCredentialCreationOptions
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

export function formatWebAuthnError(error: unknown): string {
  if (!(error instanceof Error)) {
    return 'Passkey 操作失败'
  }
  const name = (error as DOMException).name
  const message = error.message || ''
  if (name === 'NotAllowedError' || message.includes('timed out') || message.includes('not allowed')) {
    return '未找到可用的 Passkey，或你已取消验证。请确认：1) 已用密码登录并在「个人中心」注册 Passkey；2) 使用与注册时相同的浏览器和设备；3) 访问地址使用 localhost 而非 127.0.0.1（或反之保持一致）'
  }
  if (name === 'SecurityError') {
    return '当前页面环境不支持 Passkey，请使用 https 或 localhost 访问'
  }
  if (name === 'InvalidStateError') {
    return '该 Passkey 已注册，请勿重复注册'
  }
  return error.message || 'Passkey 操作失败'
}
