import { http, unwrap } from './http'
import type {
  AuditLog,
  AuthorizationInfo,
  ClientInfo,
  CreateClientPayload,
  CreateClientResult,
  CreateIdentityProviderPayload,
  CreateRolePayload,
  CreateTenantPayload,
  CreateUserPayload,
  DashboardStats,
  IdentityProviderInfo,
  LdapProviderInfo,
  LoginResult,
  GroupInfo,
  CreateGroupPayload,
  UpdateGroupPayload,
  CreateLdapProviderPayload,
  UpdateLdapProviderPayload,
  PasswordPolicyInfo,
  MfaSetupInfo,
  PageResult,
  RoleInfo,
  SessionInfo,
  SettingsInfo,
  TenantInfo,
  UpdateClientPayload,
  UpdateIdentityProviderPayload,
  UpdateRolePayload,
  UpdateTenantPayload,
  UpdateUserPayload,
  UserInfo,
} from '@/types'

export const authApi = {
  login(username: string, password: string) {
    return unwrap<LoginResult>(http.post('/api/admin/auth/login', { username, password }))
  },
  verifyMfa(mfaToken: string, code: string) {
    return unwrap<UserInfo>(http.post('/api/admin/auth/mfa/verify', { mfaToken, code }))
  },
  logout() {
    return unwrap(http.post('/api/admin/auth/logout'))
  },
  me() {
    return unwrap<UserInfo>(http.get('/api/admin/auth/me'))
  },
  changePassword(currentPassword: string, newPassword: string) {
    return unwrap(http.post('/api/admin/auth/change-password', { currentPassword, newPassword }))
  },
  mfaSetup() {
    return unwrap<MfaSetupInfo>(http.get('/api/admin/auth/mfa/setup'))
  },
  mfaEnable(code: string) {
    return unwrap(http.post('/api/admin/auth/mfa/enable', { code }))
  },
  mfaDisable(code: string) {
    return unwrap(http.post('/api/admin/auth/mfa/disable', { code }))
  },
  webauthnList() {
    return unwrap<WebauthnCredentialInfo[]>(http.get('/api/admin/auth/webauthn/credentials'))
  },
  webauthnRegisterOptions(nickname = 'Passkey') {
    return unwrap<WebauthnOptions>(http.post('/api/admin/auth/webauthn/register/options', { nickname }))
  },
  webauthnRegisterFinish(sessionToken: string, credential: Record<string, unknown>, nickname?: string) {
    return unwrap(http.post('/api/admin/auth/webauthn/register/finish', { sessionToken, credential, nickname }))
  },
  webauthnDelete(id: number) {
    return unwrap(http.delete(`/api/admin/auth/webauthn/credentials/${id}`))
  },
  webauthnLoginOptions(username?: string) {
    return unwrap<WebauthnOptions>(http.post('/api/public/webauthn/login/options', username ? { username } : {}))
  },
  webauthnLoginFinish(sessionToken: string, credential: Record<string, unknown>) {
    return unwrap<{ redirectUrl: string }>(http.post('/api/public/webauthn/login/finish', { sessionToken, credential }))
  },
}

export const dashboardApi = {
  stats() {
    return unwrap<DashboardStats>(http.get('/api/admin/dashboard/stats'))
  },
}

export const settingsApi = {
  get() {
    return unwrap<SettingsInfo>(http.get('/api/admin/settings'))
  },
  updatePasswordPolicy(payload: PasswordPolicyInfo) {
    return unwrap<PasswordPolicyInfo>(http.put('/api/admin/settings/password-policy', payload))
  },
}

export const groupApi = {
  list(page = 1, size = 20, keyword?: string) {
    return unwrap<PageResult<GroupInfo>>(http.get('/api/admin/groups', { params: { page, size, keyword } }))
  },
  listAll() {
    return unwrap<GroupInfo[]>(http.get('/api/admin/groups/all'))
  },
  create(payload: CreateGroupPayload) {
    return unwrap<GroupInfo>(http.post('/api/admin/groups', payload))
  },
  update(id: number, payload: UpdateGroupPayload) {
    return unwrap<GroupInfo>(http.put(`/api/admin/groups/${id}`, payload))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/groups/${id}`))
  },
}

export const ldapApi = {
  list(page = 1, size = 20) {
    return unwrap<PageResult<LdapProviderInfo>>(http.get('/api/admin/ldap-providers', { params: { page, size } }))
  },
  create(payload: CreateLdapProviderPayload) {
    return unwrap<LdapProviderInfo>(http.post('/api/admin/ldap-providers', payload))
  },
  update(id: number, payload: UpdateLdapProviderPayload) {
    return unwrap<LdapProviderInfo>(http.put(`/api/admin/ldap-providers/${id}`, payload))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/ldap-providers/${id}`))
  },
  test(id: number) {
    return unwrap<string>(http.post(`/api/admin/ldap-providers/${id}/test`))
  },
}

export const sessionApi = {
  list(username?: string) {
    return unwrap<SessionInfo[]>(http.get('/api/admin/sessions', { params: { username } }))
  },
  revoke(sessionId: string) {
    return unwrap(http.delete(`/api/admin/sessions/${sessionId}`))
  },
}

export const authorizationApi = {
  list(page = 1, size = 20, principalName?: string, clientId?: string) {
    return unwrap<PageResult<AuthorizationInfo>>(http.get('/api/admin/authorizations', {
      params: { page, size, principalName, clientId },
    }))
  },
  revoke(id: string) {
    return unwrap(http.delete(`/api/admin/authorizations/${id}`))
  },
}

export const identityProviderApi = {
  list(page = 1, size = 20) {
    return unwrap<PageResult<IdentityProviderInfo>>(http.get('/api/admin/identity-providers', { params: { page, size } }))
  },
  create(payload: CreateIdentityProviderPayload) {
    return unwrap<IdentityProviderInfo>(http.post('/api/admin/identity-providers', payload))
  },
  update(id: number, payload: UpdateIdentityProviderPayload) {
    return unwrap<IdentityProviderInfo>(http.put(`/api/admin/identity-providers/${id}`, payload))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/identity-providers/${id}`))
  },
  parseSamlMetadata(metadataUri: string) {
    return unwrap<{ entityId: string; ssoUrl: string; verificationCertificate: string; metadataUri?: string }>(
      http.post('/api/admin/identity-providers/parse-saml-metadata', { metadataUri }),
    )
  },
}

export const clientApi = {
  list(page = 1, size = 20) {
    return unwrap<PageResult<ClientInfo>>(http.get('/api/admin/clients', { params: { page, size } }))
  },
  get(clientId: string) {
    return unwrap<ClientInfo>(http.get(`/api/admin/clients/${clientId}`))
  },
  create(payload: CreateClientPayload) {
    return unwrap<CreateClientResult>(http.post('/api/admin/clients', payload))
  },
  update(clientId: string, payload: UpdateClientPayload) {
    return unwrap<ClientInfo>(http.put(`/api/admin/clients/${clientId}`, payload))
  },
  enable(clientId: string) {
    return unwrap(http.post(`/api/admin/clients/${clientId}/enable`))
  },
  disable(clientId: string) {
    return unwrap(http.post(`/api/admin/clients/${clientId}/disable`))
  },
  resetSecret(clientId: string) {
    return unwrap<CreateClientResult>(http.post(`/api/admin/clients/${clientId}/reset-secret`))
  },
  remove(clientId: string) {
    return unwrap(http.delete(`/api/admin/clients/${clientId}`))
  },
}

export const userApi = {
  list(page = 1, size = 20, keyword?: string) {
    return unwrap<PageResult<UserInfo>>(http.get('/api/admin/users', { params: { page, size, keyword } }))
  },
  get(id: number) {
    return unwrap<UserInfo>(http.get(`/api/admin/users/${id}`))
  },
  create(payload: CreateUserPayload) {
    return unwrap<UserInfo>(http.post('/api/admin/users', payload))
  },
  update(id: number, payload: UpdateUserPayload) {
    return unwrap<UserInfo>(http.put(`/api/admin/users/${id}`, payload))
  },
  disable(id: number) {
    return unwrap(http.post(`/api/admin/users/${id}/disable`))
  },
  enable(id: number) {
    return unwrap(http.post(`/api/admin/users/${id}/enable`))
  },
  unlock(id: number) {
    return unwrap(http.post(`/api/admin/users/${id}/unlock`))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/users/${id}`))
  },
  resetPassword(id: number, newPassword: string) {
    return unwrap(http.post(`/api/admin/users/${id}/reset-password`, { newPassword }))
  },
  resetMfa(id: number) {
    return unwrap(http.post(`/api/admin/users/${id}/reset-mfa`))
  },
}

export const tenantApi = {
  list(page = 1, size = 50) {
    return unwrap<PageResult<TenantInfo>>(http.get('/api/admin/tenants', { params: { page, size } }))
  },
  get(id: number) {
    return unwrap<TenantInfo>(http.get(`/api/admin/tenants/${id}`))
  },
  create(payload: CreateTenantPayload) {
    return unwrap<TenantInfo>(http.post('/api/admin/tenants', payload))
  },
  update(id: number, payload: UpdateTenantPayload) {
    return unwrap<TenantInfo>(http.put(`/api/admin/tenants/${id}`, payload))
  },
  enable(id: number) {
    return unwrap(http.post(`/api/admin/tenants/${id}/enable`))
  },
  disable(id: number) {
    return unwrap(http.post(`/api/admin/tenants/${id}/disable`))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/tenants/${id}`))
  },
}

export const roleApi = {
  list() {
    return unwrap<RoleInfo[]>(http.get('/api/admin/roles'))
  },
  get(id: number) {
    return unwrap<RoleInfo>(http.get(`/api/admin/roles/${id}`))
  },
  create(payload: CreateRolePayload) {
    return unwrap<RoleInfo>(http.post('/api/admin/roles', payload))
  },
  update(id: number, payload: UpdateRolePayload) {
    return unwrap<RoleInfo>(http.put(`/api/admin/roles/${id}`, payload))
  },
  remove(id: number) {
    return unwrap(http.delete(`/api/admin/roles/${id}`))
  },
}

export const auditApi = {
  list(page = 1, size = 20, eventType?: string, actor?: string, startTime?: string, endTime?: string) {
    return unwrap<PageResult<AuditLog>>(http.get('/api/admin/audit-logs', {
      params: { page, size, eventType, actor, startTime, endTime },
    }))
  },
  exportUrl(eventType?: string, actor?: string, startTime?: string, endTime?: string) {
    const params = new URLSearchParams()
    if (eventType) params.set('eventType', eventType)
    if (actor) params.set('actor', actor)
    if (startTime) params.set('startTime', startTime)
    if (endTime) params.set('endTime', endTime)
    const qs = params.toString()
    return `/api/admin/audit-logs/export${qs ? `?${qs}` : ''}`
  },
}
