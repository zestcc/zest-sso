<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { webhookApi } from '@/api/admin'
import type { WebhookDeliveryInfo } from '@/types'

const loading = ref(false)
const deliveries = ref<WebhookDeliveryInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

async function load() {
  loading.value = true
  try {
    const result = await webhookApi.list(page.value, pageSize.value)
    deliveries.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败（需启用 webhooks/alerts 模块）')
  } finally {
    loading.value = false
  }
}

async function retry(record: WebhookDeliveryInfo) {
  try {
    await webhookApi.retry(record.id)
    message.success('已触发重试')
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '重试失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <a-typography-title :level="4">Webhook 投递</a-typography-title>
    <a-alert
      type="info"
      show-icon
      message="需 zest.sso.modules.webhooks 或 alerts 启用后才有投递记录。"
      style="margin-bottom: 16px"
    />
    <a-table
      :data-source="deliveries"
      :loading="loading"
      row-key="id"
      :pagination="{ current: page, pageSize, total, onChange: (p: number, s: number) => { page = p; pageSize = s; load() } }"
    >
      <a-table-column title="事件" data-index="eventType" key="eventType" width="140" />
      <a-table-column title="端点" data-index="endpointUrl" key="endpointUrl" />
      <a-table-column title="状态" data-index="status" key="status" width="100" />
      <a-table-column title="次数" data-index="attemptCount" key="attemptCount" width="70" />
      <a-table-column title="时间" data-index="createTime" key="createTime" width="180" />
      <a-table-column title="操作" key="actions" width="100">
        <template #default="{ record }">
          <a-button v-if="record.status !== 'SUCCESS'" type="link" size="small" @click="retry(record)">重试</a-button>
        </template>
      </a-table-column>
    </a-table>
  </div>
</template>
