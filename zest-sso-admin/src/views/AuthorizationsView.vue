<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { authorizationApi } from '@/api/admin'
import type { AuthorizationInfo } from '@/types'

const loading = ref(false)
const records = ref<AuthorizationInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const principalName = ref('')
const clientId = ref('')

async function loadData() {
  loading.value = true
  try {
    const result = await authorizationApi.list(page.value, pageSize.value, principalName.value || undefined, clientId.value || undefined)
    records.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function confirmRevoke(record: AuthorizationInfo) {
  Modal.confirm({
    title: '吊销授权',
    content: `确定吊销 ${record.principalName} 在 ${record.clientId} 的 Token？`,
    okType: 'danger',
    onOk: async () => {
      await authorizationApi.revoke(record.id)
      message.success('授权已吊销')
      await loadData()
    },
  })
}

onMounted(loadData)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-space wrap>
        <a-input v-model:value="principalName" placeholder="用户名" style="width: 180px" />
        <a-input v-model:value="clientId" placeholder="Client ID" style="width: 180px" />
        <a-button type="primary" @click="() => { page = 1; loadData() }">查询</a-button>
      </a-space>
    </div>
    <a-table
      :data-source="records"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page, pageSize, total, showSizeChanger: true,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadData() },
      }"
    >
      <a-table-column title="用户" data-index="principalName" key="principalName" />
      <a-table-column title="Client ID" data-index="clientId" key="clientId" />
      <a-table-column title="授权类型" data-index="grantType" key="grantType" />
      <a-table-column title="Scopes" data-index="scopes" key="scopes" />
      <a-table-column title="Access Token 过期" key="accessTokenExpiresAt">
        <template #default="{ record }">
          {{ record.accessTokenExpiresAt ? dayjs(record.accessTokenExpiresAt).format('YYYY-MM-DD HH:mm') : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="状态" key="active" width="90">
        <template #default="{ record }">
          <a-badge :status="record.active ? 'success' : 'default'" :text="record.active ? '有效' : '过期'" />
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="100">
        <template #default="{ record }">
          <a-button type="link" danger size="small" @click="confirmRevoke(record)">吊销</a-button>
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>
