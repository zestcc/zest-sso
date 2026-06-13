<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { auditApi } from '@/api/admin'
import type { AuditLog } from '@/types'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const eventType = ref<string>()
const actor = ref('')
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const eventOptions = [
  { label: '全部', value: undefined },
  { label: '登录成功', value: 'LOGIN_SUCCESS' },
  { label: '登录失败', value: 'LOGIN_FAILURE' },
  { label: '登出', value: 'LOGOUT' },
  { label: '创建用户', value: 'USER_CREATE' },
  { label: '更新用户', value: 'USER_UPDATE' },
  { label: '禁用用户', value: 'USER_DISABLE' },
  { label: '启用用户', value: 'USER_ENABLE' },
  { label: '解锁用户', value: 'USER_UNLOCK' },
  { label: '删除用户', value: 'USER_DELETE' },
  { label: '修改密码', value: 'PASSWORD_CHANGE' },
  { label: '创建客户端', value: 'CLIENT_CREATE' },
  { label: '更新客户端', value: 'CLIENT_UPDATE' },
  { label: '禁用客户端', value: 'CLIENT_DISABLE' },
  { label: '启用客户端', value: 'CLIENT_ENABLE' },
  { label: '删除客户端', value: 'CLIENT_DELETE' },
  { label: '重置密钥', value: 'CLIENT_RESET_SECRET' },
  { label: '创建租户', value: 'TENANT_CREATE' },
  { label: '更新租户', value: 'TENANT_UPDATE' },
  { label: '禁用租户', value: 'TENANT_DISABLE' },
  { label: '启用租户', value: 'TENANT_ENABLE' },
  { label: '删除租户', value: 'TENANT_DELETE' },
  { label: '创建角色', value: 'ROLE_CREATE' },
  { label: '更新角色', value: 'ROLE_UPDATE' },
  { label: '删除角色', value: 'ROLE_DELETE' },
  { label: '吊销令牌', value: 'TOKEN_REVOKE' },
  { label: '强制下线', value: 'SESSION_REVOKE' },
  { label: '启用 MFA', value: 'MFA_ENABLE' },
  { label: '禁用 MFA', value: 'MFA_DISABLE' },
  { label: '管理员重置 MFA', value: 'MFA_ADMIN_RESET' },
  { label: '创建身份源', value: 'IDP_CREATE' },
  { label: '更新身份源', value: 'IDP_UPDATE' },
  { label: '删除身份源', value: 'IDP_DELETE' },
]

async function loadLogs() {
  loading.value = true
  try {
    const startTime = dateRange.value?.[0]?.format('YYYY-MM-DD HH:mm:ss')
    const endTime = dateRange.value?.[1]?.format('YYYY-MM-DD HH:mm:ss')
    const result = await auditApi.list(page.value, pageSize.value, eventType.value, actor.value || undefined, startTime, endTime)
    logs.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)

function exportCsv() {
  const startTime = dateRange.value?.[0]?.format('YYYY-MM-DD HH:mm:ss')
  const endTime = dateRange.value?.[1]?.format('YYYY-MM-DD HH:mm:ss')
  window.open(auditApi.exportUrl(eventType.value, actor.value || undefined, startTime, endTime), '_blank')
}
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-space wrap>
        <a-select
          v-model:value="eventType"
          style="width: 180px"
          placeholder="事件类型"
          :options="eventOptions"
          allow-clear
        />
        <a-input
          v-model:value="actor"
          placeholder="操作者"
          style="width: 200px"
          @press-enter="() => { page = 1; loadLogs() }"
        />
        <a-range-picker
          v-model:value="dateRange"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
        />
        <a-button type="primary" @click="() => { page = 1; loadLogs() }">查询</a-button>
        <a-button @click="exportCsv">导出 CSV</a-button>
      </a-space>
    </div>

    <a-table
      :data-source="logs"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadLogs() },
      }"
    >
      <a-table-column title="时间" data-index="createTime" key="createTime" width="180">
        <template #default="{ text }">
          {{ text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="事件" data-index="eventType" key="eventType" width="150" />
      <a-table-column title="操作者" data-index="actor" key="actor" width="120" />
      <a-table-column title="目标" data-index="target" key="target" width="140" />
      <a-table-column title="客户端" data-index="clientId" key="clientId" width="140" />
      <a-table-column title="IP" data-index="ipAddress" key="ipAddress" width="130" />
      <a-table-column title="详情" data-index="detail" key="detail" />
    </a-table>
  </div>
</template>
