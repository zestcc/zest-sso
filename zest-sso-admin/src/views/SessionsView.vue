<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { sessionApi } from '@/api/admin'
import type { SessionInfo } from '@/types'

const loading = ref(false)
const sessions = ref<SessionInfo[]>([])
const username = ref('')

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await sessionApi.list(username.value || undefined)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function confirmRevoke(record: SessionInfo) {
  Modal.confirm({
    title: '强制下线',
    content: `确定强制下线用户 ${record.username || record.sessionId} 的会话？`,
    okType: 'danger',
    onOk: async () => {
      await sessionApi.revoke(record.sessionId)
      message.success('会话已终止')
      await loadSessions()
    },
  })
}

onMounted(loadSessions)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-input-search
        v-model:value="username"
        placeholder="按用户名筛选"
        style="width: 260px"
        @search="loadSessions"
      />
      <a-button @click="loadSessions">刷新</a-button>
    </div>
    <a-table :data-source="sessions" :loading="loading" row-key="sessionId" :pagination="false">
      <a-table-column title="Session ID" data-index="sessionId" key="sessionId" />
      <a-table-column title="用户" data-index="username" key="username" />
      <a-table-column title="创建时间" key="creationTime">
        <template #default="{ record }">{{ dayjs(record.creationTime).format('YYYY-MM-DD HH:mm:ss') }}</template>
      </a-table-column>
      <a-table-column title="最后访问" key="lastAccessedTime">
        <template #default="{ record }">{{ dayjs(record.lastAccessedTime).format('YYYY-MM-DD HH:mm:ss') }}</template>
      </a-table-column>
      <a-table-column title="状态" key="expired" width="100">
        <template #default="{ record }">
          <a-badge :status="record.expired ? 'default' : 'success'" :text="record.expired ? '已过期' : '活跃'" />
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="120">
        <template #default="{ record }">
          <a-button type="link" danger size="small" @click="confirmRevoke(record)">强制下线</a-button>
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>
