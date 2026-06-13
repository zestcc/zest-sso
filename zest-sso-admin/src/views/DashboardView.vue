<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { dashboardApi } from '@/api/admin'
import type { DashboardStats } from '@/types'

const loading = ref(false)
const stats = ref<DashboardStats | null>(null)

const endpoints = [
  { label: 'OIDC Discovery', path: '/.well-known/openid-configuration' },
  { label: 'Authorize', path: '/oauth2/authorize' },
  { label: 'Token', path: '/oauth2/token' },
  { label: 'JWKS', path: '/oauth2/jwks' },
  { label: 'UserInfo', path: '/userinfo' },
  { label: 'Revoke', path: '/oauth2/revoke' },
]

async function loadStats() {
  loading.value = true
  try {
    stats.value = await dashboardApi.stats()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <a-spin :spinning="loading">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="label">用户总数</div>
          <div class="value">{{ stats?.totalUsers ?? '-' }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="label">活跃用户</div>
          <div class="value">{{ stats?.activeUsers ?? '-' }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="label">接入应用</div>
          <div class="value">{{ stats?.activeClients ?? '-' }} / {{ stats?.totalClients ?? '-' }}</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card">
          <div class="label">24h 登录失败</div>
          <div class="value" style="color: #cf1322">{{ stats?.loginFailure24h ?? '-' }}</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :xs="24" :lg="12">
        <div class="page-card">
          <h3>身份提供商</h3>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="Issuer">{{ stats?.issuer || '-' }}</a-descriptions-item>
            <a-descriptions-item label="租户数">{{ stats?.totalTenants ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="24h 登录成功">{{ stats?.loginSuccess24h ?? '-' }}</a-descriptions-item>
          </a-descriptions>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="page-card">
          <h3>OIDC 端点</h3>
          <a-list size="small" bordered :data-source="endpoints">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-typography-text code>{{ stats?.issuer }}{{ item.path }}</a-typography-text>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-col>
    </a-row>

    <div class="page-card" style="margin-top: 16px">
      <h3>最近审计事件</h3>
      <a-table
        :data-source="stats?.recentAuditLogs || []"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <a-table-column title="时间" data-index="createTime" key="createTime">
          <template #default="{ text }">
            {{ text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
        </a-table-column>
        <a-table-column title="事件" data-index="eventType" key="eventType" />
        <a-table-column title="操作者" data-index="actor" key="actor" />
        <a-table-column title="目标" data-index="target" key="target" />
        <a-table-column title="IP" data-index="ipAddress" key="ipAddress" />
      </a-table>
    </div>
  </a-spin>
</template>

<style scoped>
h3 {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
