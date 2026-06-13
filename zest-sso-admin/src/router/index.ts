import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  canManageClients,
  canManageRoles,
  canManageTenants,
  canManageUsers,
  canViewAudit,
  canViewSettings,
} from '@/utils/permissions'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '概览' } },
        { path: 'clients', name: 'clients', component: () => import('@/views/ClientsView.vue'), meta: { title: '应用接入', requiresAdmin: true } },
        { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '用户管理', requiresUserMgmt: true } },
        { path: 'tenants', name: 'tenants', component: () => import('@/views/TenantsView.vue'), meta: { title: '租户管理', requiresAdmin: true } },
        { path: 'roles', name: 'roles', component: () => import('@/views/RolesView.vue'), meta: { title: '角色管理', requiresAdmin: true } },
        { path: 'groups', name: 'groups', component: () => import('@/views/GroupsView.vue'), meta: { title: '用户组', requiresAdmin: true } },
        { path: 'audit-logs', name: 'audit-logs', component: () => import('@/views/AuditLogsView.vue'), meta: { title: '审计日志', requiresAudit: true } },
        { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '系统设置', requiresAdmin: true } },
        { path: 'sessions', name: 'sessions', component: () => import('@/views/SessionsView.vue'), meta: { title: '会话管理', requiresAdmin: true } },
        { path: 'authorizations', name: 'authorizations', component: () => import('@/views/AuthorizationsView.vue'), meta: { title: '授权令牌', requiresAdmin: true } },
        { path: 'identity-providers', name: 'identity-providers', component: () => import('@/views/IdentityProvidersView.vue'), meta: { title: '身份联邦', requiresAdmin: true } },
        { path: 'ldap-providers', name: 'ldap-providers', component: () => import('@/views/LdapProvidersView.vue'), meta: { title: 'LDAP/AD', requiresAdmin: true } },
        { path: 'modules', name: 'modules', component: () => import('@/views/ModulesView.vue'), meta: { title: '可插拔模块', requiresAdmin: true } },
        { path: 'webhooks', name: 'webhooks', component: () => import('@/views/WebhooksView.vue'), meta: { title: 'Webhook', requiresAdmin: true } },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心' } },
      ],
    },
  ],
})

function canAccessRoute(meta: Record<string, unknown>, user: ReturnType<typeof useAuthStore>['user']) {
  if (meta.requiresAdmin && !canManageClients(user)) return false
  if (meta.requiresUserMgmt && !canManageUsers(user)) return false
  if (meta.requiresAudit && !canViewAudit(user)) return false
  if (meta.title === '租户管理' && !canManageTenants(user)) return false
  if (meta.title === '角色管理' && !canManageRoles(user)) return false
  if (meta.title === '系统设置' && !canViewSettings(user)) return false
  return true
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.user && to.name === 'login') {
      return { name: 'dashboard' }
    }
    return true
  }

  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (!canAccessRoute(to.meta, auth.user)) {
    return { name: 'forbidden' }
  }
  return true
})

export default router
