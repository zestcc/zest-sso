import type { UserInfo } from '@/types'

export const ROLE_SSO_ADMIN = 'SSO_ADMIN'
export const ROLE_SSO_OPERATOR = 'SSO_OPERATOR'

export function isSsoAdmin(user: UserInfo | null | undefined) {
  return !!user?.roles?.includes(ROLE_SSO_ADMIN)
}

export function isSsoOperator(user: UserInfo | null | undefined) {
  return !!user?.roles?.includes(ROLE_SSO_OPERATOR)
}

export function canManageClients(user: UserInfo | null | undefined) {
  return isSsoAdmin(user)
}

export function canManageTenants(user: UserInfo | null | undefined) {
  return isSsoAdmin(user)
}

export function canManageRoles(user: UserInfo | null | undefined) {
  return isSsoAdmin(user)
}

export function canViewSettings(user: UserInfo | null | undefined) {
  return isSsoAdmin(user)
}

export function canManageUsers(user: UserInfo | null | undefined) {
  return isSsoAdmin(user) || isSsoOperator(user)
}

export function canViewAudit(user: UserInfo | null | undefined) {
  return isSsoAdmin(user) || isSsoOperator(user)
}
