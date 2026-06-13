<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ApiOutlined,
  AppstoreOutlined,
  AuditOutlined,
  CloudOutlined,
  DashboardOutlined,
  KeyOutlined,
  LogoutOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UsergroupAddOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  canManageClients,
  canManageRoles,
  canManageTenants,
  canManageUsers,
  canViewAudit,
  canViewSettings,
} from '@/utils/permissions'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const selectedKeys = computed(() => [route.name as string])

const menuItems = computed(() => {
  const user = auth.user
  const items = [
    { key: 'dashboard', icon: DashboardOutlined, label: '概览', path: '/dashboard', visible: true },
    { key: 'clients', icon: AppstoreOutlined, label: '应用接入', path: '/clients', visible: canManageClients(user) },
    { key: 'users', icon: UserOutlined, label: '用户管理', path: '/users', visible: canManageUsers(user) },
    { key: 'tenants', icon: TeamOutlined, label: '租户管理', path: '/tenants', visible: canManageTenants(user) },
    { key: 'roles', icon: SafetyCertificateOutlined, label: '角色管理', path: '/roles', visible: canManageRoles(user) },
    { key: 'groups', icon: UsergroupAddOutlined, label: '用户组', path: '/groups', visible: canManageRoles(user) },
    { key: 'identity-providers', icon: CloudOutlined, label: '身份联邦', path: '/identity-providers', visible: canManageClients(user) },
    { key: 'ldap-providers', icon: TeamOutlined, label: 'LDAP/AD', path: '/ldap-providers', visible: canManageClients(user) },
    { key: 'modules', icon: ApiOutlined, label: '可插拔模块', path: '/modules', visible: canManageClients(user) },
    { key: 'webhooks', icon: ApiOutlined, label: 'Webhook', path: '/webhooks', visible: canManageClients(user) },
    { key: 'sessions', icon: ApiOutlined, label: '会话管理', path: '/sessions', visible: canManageClients(user) },
    { key: 'authorizations', icon: KeyOutlined, label: '授权令牌', path: '/authorizations', visible: canManageClients(user) },
    { key: 'audit-logs', icon: AuditOutlined, label: '审计日志', path: '/audit-logs', visible: canViewAudit(user) },
    { key: 'settings', icon: SettingOutlined, label: '系统设置', path: '/settings', visible: canViewSettings(user) },
  ]
  return items.filter((item) => item.visible)
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      theme="dark"
      width="240"
      :style="{ background: '#141b2d' }"
    >
      <div class="brand">
        <div class="brand-logo">Z</div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-title">ZestSSO</div>
          <div class="brand-subtitle">Identity Admin</div>
        </div>
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :style="{ background: 'transparent', border: 'none' }"
      >
        <a-menu-item v-for="item in menuItems" :key="item.key" @click="router.push(item.path)">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-title">{{ route.meta.title }}</div>
        <div class="header-user">
          <a-dropdown>
            <div class="user-trigger">
              <a-avatar :size="32" style="background: #4c51bf">
                {{ auth.user?.displayName?.charAt(0) || 'A' }}
              </a-avatar>
              <div class="user-meta">
                <div class="user-name">{{ auth.user?.displayName }}</div>
                <div class="user-role">{{ auth.user?.roles?.join(', ') }}</div>
              </div>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/profile')">个人中心</a-menu-item>
                <a-menu-item @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button type="text" @click="handleLogout">
            <LogoutOutlined />
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px 24px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.brand-title {
  color: #fff;
  font-weight: 600;
}

.brand-subtitle {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.user-meta {
  line-height: 1.2;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  color: #8c8c8c;
}

.content {
  margin: 24px;
}
</style>
