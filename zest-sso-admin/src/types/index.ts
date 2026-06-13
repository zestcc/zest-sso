export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  email: string
  displayName: string
  status: number
  superAdmin: boolean
  roles: string[]
  groups?: string[]
  groupIds?: number[]
  tenants: TenantInfo[]
  defaultTenantId: number | null
  lastLoginAt?: string | null
  lastLoginIp?: string | null
  mfaEnabled?: boolean
}

export interface LoginResult {
  mfaRequired: boolean
  mfaToken?: string
  user?: UserInfo
}

export interface MfaSetupInfo {
  secret: string
  otpAuthUrl: string
  enabled: boolean
}

export interface WebauthnCredentialInfo {
  id: number
  nickname: string
  transports?: string
  createTime: string
  lastUsedAt?: string | null
}

export interface WebauthnOptions {
  sessionToken: string
  publicKey: Record<string, unknown>
}

export interface SessionInfo {
  sessionId: string
  username: string
  creationTime: string
  lastAccessedTime: string
  maxInactiveIntervalSeconds: number
  expired: boolean
}

export interface AuthorizationInfo {
  id: string
  clientId: string
  principalName: string
  grantType: string
  scopes: string
  accessTokenExpiresAt?: string | null
  refreshTokenExpiresAt?: string | null
  active: boolean
}

export interface IdentityProviderInfo {
  id?: number
  alias: string
  displayName: string
  providerType?: string
  discoveryUri?: string
  clientId?: string
  scopes?: string
  usernameClaim?: string
  emailClaim?: string
  displayNameClaim?: string
  roleClaim?: string
  defaultRoleCodes?: string
  samlMetadataUri?: string
  samlEntityId?: string
  samlSsoUrl?: string
  enabled?: number
  loginUrl?: string
}

export interface GroupInfo {
  id: number
  code: string
  name: string
  description?: string
  roleCodes?: string[]
  memberCount?: number
}

export interface LdapProviderInfo {
  id?: number
  alias: string
  displayName: string
  serverUrl: string
  baseDn: string
  bindDn?: string
  userSearchBase: string
  userSearchFilter?: string
  groupSearchBase?: string
  groupRoleAttribute?: string
  enabled?: number
}

export interface PasswordPolicyInfo {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireDigit: boolean
  requireSpecial: boolean
  passwordHistoryCount: number
  maxAgeDays: number
}

export interface CreateIdentityProviderPayload {
  alias: string
  displayName: string
  providerType?: 'OIDC' | 'SAML'
  discoveryUri?: string
  clientId?: string
  clientSecret?: string
  scopes?: string
  usernameClaim?: string
  emailClaim?: string
  displayNameClaim?: string
  roleClaim?: string
  defaultRoleCodes?: string
  samlMetadataUri?: string
  samlEntityId?: string
  samlSsoUrl?: string
  samlVerificationCertificate?: string
}

export interface UpdateIdentityProviderPayload {
  displayName?: string
  discoveryUri?: string
  clientId?: string
  clientSecret?: string
  scopes?: string
  usernameClaim?: string
  emailClaim?: string
  displayNameClaim?: string
  roleClaim?: string
  defaultRoleCodes?: string
  samlMetadataUri?: string
  samlEntityId?: string
  samlSsoUrl?: string
  samlVerificationCertificate?: string
  enabled?: number
}

export interface TenantInfo {
  id: number
  code: string
  name: string
  status?: number
  isDefault: boolean
  system?: boolean
}

export interface RoleInfo {
  id: number
  code: string
  name: string
  description: string
  system?: boolean
}

export interface ClientInfo {
  id: number
  clientId: string
  clientName: string
  authorizationGrantTypes: string[]
  redirectUris: string[]
  scopes: string[]
  requirePkce: boolean
  requireConsent?: boolean
  accessTokenTtl: number
  refreshTokenTtl: number
  backchannelLogoutUri?: string
  frontchannelLogoutUri?: string
  status: number
}

export interface CreateClientResult extends ClientInfo {
  clientSecret?: string
}

export interface AuditLog {
  id: number
  eventType: string
  actor: string
  target: string
  clientId: string
  ipAddress: string
  userAgent: string
  detail: string
  createTime: string
}

export interface DashboardStats {
  totalUsers: number
  activeUsers: number
  totalClients: number
  activeClients: number
  totalTenants: number
  loginSuccess24h: number
  loginFailure24h: number
  issuer: string
  recentAuditLogs: AuditLog[]
}

export interface SettingsInfo {
  issuer: string
  keyId: string
  accessTokenTtl: number
  refreshTokenTtl: number
  idTokenTtl: number
  loginRateLimit: number
  loginRateWindowSeconds: number
  maxLoginAttempts: number
  loginLockMinutes: number
  adminConsolePath: string
  passwordPolicy?: PasswordPolicyInfo
}

export interface CreateUserPayload {
  username: string
  email: string
  password: string
  displayName?: string
  roleCodes: string[]
  tenantIds: number[]
  defaultTenantId?: number
  groupIds?: number[]
}

export interface UpdateUserPayload {
  email?: string
  displayName?: string
  status?: number
  roleCodes?: string[]
  tenantIds?: number[]
  defaultTenantId?: number
  groupIds?: number[]
}

export interface CreateClientPayload {
  clientId: string
  clientSecret: string
  clientName: string
  authorizationGrantTypes?: string[]
  redirectUris?: string[]
  scopes?: string[]
  requirePkce?: boolean
  requireConsent?: boolean
  accessTokenTtl?: number
  refreshTokenTtl?: number
  backchannelLogoutUri?: string
  frontchannelLogoutUri?: string
}

export interface UpdateClientPayload {
  clientName?: string
  authorizationGrantTypes?: string[]
  redirectUris?: string[]
  scopes?: string[]
  requirePkce?: boolean
  requireConsent?: boolean
  accessTokenTtl?: number
  refreshTokenTtl?: number
  backchannelLogoutUri?: string
  frontchannelLogoutUri?: string
}

export interface CreateTenantPayload {
  code: string
  name: string
}

export interface UpdateTenantPayload {
  name?: string
  status?: number
}

export interface CreateRolePayload {
  code: string
  name: string
  description?: string
}

export interface UpdateRolePayload {
  name?: string
  description?: string
}

export interface CreateGroupPayload {
  code: string
  name: string
  description?: string
  roleCodes?: string[]
}

export interface UpdateGroupPayload {
  name?: string
  description?: string
  roleCodes?: string[]
}

export interface CreateLdapProviderPayload {
  alias: string
  displayName: string
  serverUrl: string
  baseDn: string
  bindDn?: string
  bindPassword?: string
  userSearchBase: string
  userSearchFilter?: string
  groupSearchBase?: string
  groupRoleAttribute?: string
}

export interface UpdateLdapProviderPayload extends Partial<CreateLdapProviderPayload> {
  enabled?: number
}
