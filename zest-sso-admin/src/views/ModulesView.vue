<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { moduleApi, channelApi } from '@/api/admin'
import type { AlertChannelInfo, MfaChannelInfo, OptionalModuleInfo } from '@/types'

const loading = ref(false)
const modules = ref<OptionalModuleInfo[]>([])
const mfaChannels = ref<MfaChannelInfo[]>([])
const alertChannels = ref<AlertChannelInfo[]>([])

async function load() {
  loading.value = true
  try {
    const [m, mf, al] = await Promise.all([
      moduleApi.list(),
      channelApi.listMfa(),
      channelApi.listAlerts(),
    ])
    modules.value = m
    mfaChannels.value = mf
    alertChannels.value = al
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <a-typography-title :level="4">可插拔模块</a-typography-title>
    <a-alert
      type="info"
      show-icon
      message="模块通过 application.yml / Helm values 开关启用，此处为只读能力清单。"
      style="margin-bottom: 16px"
    />
    <a-spin :spinning="loading">
      <a-table :data-source="modules" row-key="key" :pagination="false" style="margin-bottom: 24px">
        <a-table-column title="模块" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" width="160" />
        <a-table-column title="分类" data-index="category" key="category" width="100" />
        <a-table-column title="状态" key="enabled" width="90">
          <template #default="{ record }">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '已启用' : '未启用'" />
          </template>
        </a-table-column>
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>

      <a-typography-title :level="5">MFA 通道</a-typography-title>
      <a-table :data-source="mfaChannels" row-key="key" :pagination="false" style="margin-bottom: 24px">
        <a-table-column title="通道" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" />
        <a-table-column title="状态" key="enabled" width="90">
          <template #default="{ record }">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '可用' : '未配置'" />
          </template>
        </a-table-column>
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>

      <a-typography-title :level="5">告警通道</a-typography-title>
      <a-table :data-source="alertChannels" row-key="key" :pagination="false">
        <a-table-column title="通道" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" />
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>
    </a-spin>
  </div>
</template>
