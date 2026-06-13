<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { alertChannelApi, channelApi } from '@/api/admin'
import type { AlertChannelConfigInfo, AlertChannelInfo } from '@/types'

const loading = ref(false)
const channels = ref<AlertChannelConfigInfo[]>([])
const channelTypes = ref<AlertChannelInfo[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive<AlertChannelConfigInfo>({
  name: '',
  channelKey: 'http-webhook',
  enabled: 1,
  events: [],
  config: {},
})

const eventOptions = [
  'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE',
  'CLIENT_CREATE', 'CLIENT_UPDATE', 'MFA_ENABLE', 'MFA_DISABLE', 'PASSWORD_RESET_COMPLETE',
]

const selectedType = computed(() => channelTypes.value.find((t) => t.key === form.channelKey))

function resetForm() {
  editingId.value = null
  form.name = ''
  form.channelKey = 'http-webhook'
  form.enabled = 1
  form.events = []
  form.config = {}
}

function openCreate() {
  resetForm()
  applyConfigHints()
  modalOpen.value = true
}

function openEdit(record: AlertChannelConfigInfo) {
  editingId.value = record.id ?? null
  form.name = record.name
  form.channelKey = record.channelKey
  form.enabled = record.enabled ?? 1
  form.events = record.events ? [...record.events] : []
  form.config = { ...(record.config ?? {}) }
  applyConfigHints()
  modalOpen.value = true
}

function applyConfigHints() {
  const hints = selectedType.value?.configHints ?? {}
  for (const key of Object.keys(hints)) {
    if (!(key in form.config!)) {
      form.config![key] = ''
    }
  }
}

async function load() {
  loading.value = true
  try {
    const [list, types] = await Promise.all([alertChannelApi.list(), channelApi.listAlerts()])
    channels.value = list
    channelTypes.value = types
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败（需启用 alerts 模块）')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.name.trim()) {
    message.error('请填写名称')
    return
  }
  try {
    if (editingId.value) {
      await alertChannelApi.update(editingId.value, { ...form })
      message.success('已更新')
    } else {
      await alertChannelApi.create({ ...form })
      message.success('已创建')
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function remove(record: AlertChannelConfigInfo) {
  if (!record.id) return
  try {
    await alertChannelApi.remove(record.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <a-typography-title :level="4" style="margin: 0">告警通道</a-typography-title>
      <a-button type="primary" @click="openCreate">新建通道</a-button>
    </div>
    <a-alert
      type="info"
      show-icon
      message="在线配置告警投递，优先于 application.yml 中的 zest.sso.alerts.channels。配置保存后立即生效。"
      style="margin-bottom: 16px"
    />
    <a-table :data-source="channels" :loading="loading" row-key="id" :pagination="false">
      <a-table-column title="名称" data-index="name" key="name" />
      <a-table-column title="通道类型" data-index="channelKey" key="channelKey" width="140" />
      <a-table-column title="状态" key="enabled" width="90">
        <template #default="{ record }">
          <a-badge :status="record.enabled === 1 ? 'success' : 'default'" :text="record.enabled === 1 ? '启用' : '禁用'" />
        </template>
      </a-table-column>
      <a-table-column title="订阅事件" key="events">
        <template #default="{ record }">
          {{ record.events?.length ? record.events.join(', ') : '全部' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="140">
        <template #default="{ record }">
          <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
          <a-popconfirm title="确认删除？" @confirm="remove(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </a-table-column>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑告警通道' : '新建告警通道'"
      ok-text="保存"
      width="640px"
      @ok="save"
    >
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" placeholder="如：运维钉钉群" />
        </a-form-item>
        <a-form-item label="通道类型" required>
          <a-select v-model:value="form.channelKey" @change="applyConfigHints">
            <a-select-option v-for="t in channelTypes" :key="t.key" :value="t.key">
              {{ t.name }} ({{ t.key }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch :checked="form.enabled === 1" @change="(v: boolean) => form.enabled = v ? 1 : 0" />
        </a-form-item>
        <a-form-item label="订阅事件（留空=全部）">
          <a-select v-model:value="form.events" mode="multiple" :options="eventOptions.map((e) => ({ value: e, label: e }))" />
        </a-form-item>
        <a-form-item
          v-for="(hint, key) in selectedType?.configHints ?? {}"
          :key="key"
          :label="hint"
        >
          <a-input
            v-model:value="form.config![key]"
            :placeholder="hint"
            :type="key.toLowerCase().includes('secret') ? 'password' : 'text'"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
