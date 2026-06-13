<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { clientApi } from '@/api/admin'
import type { ClientInfo, CreateClientPayload, UpdateClientPayload } from '@/types'

const loading = ref(false)
const clients = ref<ClientInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const modalOpen = ref(false)
const editing = ref<ClientInfo | null>(null)
const secretModalOpen = ref(false)
const revealedSecret = ref('')

const defaultScopes = ['openid', 'profile', 'email', 'roles', 'tenant', 'scim']
const defaultGrants = ['authorization_code', 'refresh_token']

const form = reactive<CreateClientPayload>({
  clientId: '',
  clientSecret: '',
  clientName: '',
  authorizationGrantTypes: [...defaultGrants],
  redirectUris: [''],
  scopes: [...defaultScopes],
  requirePkce: true,
  requireConsent: false,
  accessTokenTtl: 3600,
  refreshTokenTtl: 86400,
  backchannelLogoutUri: '',
  frontchannelLogoutUri: '',
})

const modalTitle = computed(() => (editing.value ? '编辑应用' : '注册新应用'))

async function loadClients() {
  loading.value = true
  try {
    const result = await clientApi.list(page.value, pageSize.value)
    clients.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editing.value = null
  form.clientId = ''
  form.clientSecret = ''
  form.clientName = ''
  form.authorizationGrantTypes = [...defaultGrants]
  form.redirectUris = ['']
  form.scopes = [...defaultScopes]
  form.requirePkce = true
  form.requireConsent = false
  form.accessTokenTtl = 3600
  form.refreshTokenTtl = 86400
  form.backchannelLogoutUri = ''
  form.frontchannelLogoutUri = ''
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: ClientInfo) {
  editing.value = record
  form.clientId = record.clientId
  form.clientName = record.clientName
  form.authorizationGrantTypes = [...record.authorizationGrantTypes]
  form.redirectUris = record.redirectUris.length ? [...record.redirectUris] : ['']
  form.scopes = [...record.scopes]
  form.requirePkce = record.requirePkce
  form.requireConsent = record.requireConsent ?? false
  form.accessTokenTtl = record.accessTokenTtl
  form.refreshTokenTtl = record.refreshTokenTtl
  form.backchannelLogoutUri = record.backchannelLogoutUri || ''
  form.frontchannelLogoutUri = record.frontchannelLogoutUri || ''
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      const payload: UpdateClientPayload = {
        clientName: form.clientName,
        authorizationGrantTypes: form.authorizationGrantTypes,
        redirectUris: (form.redirectUris || []).filter(Boolean),
        scopes: form.scopes,
        requirePkce: form.requirePkce,
        requireConsent: form.requireConsent,
        accessTokenTtl: form.accessTokenTtl,
        refreshTokenTtl: form.refreshTokenTtl,
        backchannelLogoutUri: form.backchannelLogoutUri || undefined,
        frontchannelLogoutUri: form.frontchannelLogoutUri || undefined,
      }
      await clientApi.update(editing.value.clientId, payload)
      message.success('应用已更新')
    } else {
      if (!form.clientSecret || form.clientSecret.length < 8) {
        message.error('Client Secret 至少 8 位')
        return
      }
      const result = await clientApi.create({
        ...form,
        redirectUris: (form.redirectUris || []).filter(Boolean),
      })
      if (result.clientSecret) {
        revealedSecret.value = result.clientSecret
        secretModalOpen.value = true
      }
      message.success('应用已注册')
    }
    modalOpen.value = false
    await loadClients()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleStatus(record: ClientInfo) {
  try {
    if (record.status === 1) {
      await clientApi.disable(record.clientId)
      message.success('应用已禁用')
    } else {
      await clientApi.enable(record.clientId)
      message.success('应用已启用')
    }
    await loadClients()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function confirmResetSecret(record: ClientInfo) {
  Modal.confirm({
    title: '重置 Client Secret',
    content: `确定重置 ${record.clientName} 的密钥？旧密钥将立即失效。`,
    okType: 'danger',
    onOk: async () => {
      const result = await clientApi.resetSecret(record.clientId)
      revealedSecret.value = result.clientSecret || ''
      secretModalOpen.value = true
      message.success('密钥已重置')
    },
  })
}

function confirmDelete(record: ClientInfo) {
  Modal.confirm({
    title: '删除应用',
    content: `确定删除 ${record.clientName}？此操作不可恢复。`,
    okType: 'danger',
    onOk: async () => {
      await clientApi.remove(record.clientId)
      message.success('应用已删除')
      await loadClients()
    },
  })
}

onMounted(loadClients)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-typography-title :level="4" style="margin: 0">OAuth 应用接入</a-typography-title>
      <a-button type="primary" @click="openCreate">注册应用</a-button>
    </div>

    <a-table
      :data-source="clients"
      :loading="loading"
      row-key="clientId"
      :pagination="{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadClients() },
      }"
    >
      <a-table-column title="Client ID" data-index="clientId" key="clientId" />
      <a-table-column title="应用名称" data-index="clientName" key="clientName" />
      <a-table-column title="回调地址" key="redirectUris">
        <template #default="{ record }">
          <a-tag v-for="uri in record.redirectUris" :key="uri">{{ uri }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="PKCE" key="requirePkce" width="80">
        <template #default="{ record }">
          <a-tag :color="record.requirePkce ? 'green' : 'default'">{{ record.requirePkce ? '是' : '否' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status" width="90">
        <template #default="{ record }">
          <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '启用' : '禁用'" />
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="280">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="toggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="link" size="small" @click="confirmResetSecret(record)">重置密钥</a-button>
            <a-button type="link" danger size="small" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="modalTitle" width="720px" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="Client ID" required>
            <a-input v-model:value="form.clientId" :disabled="!!editing" placeholder="my-app" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="应用名称" required>
            <a-input v-model:value="form.clientName" placeholder="我的应用" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item v-if="!editing" label="Client Secret" required>
        <a-input-password v-model:value="form.clientSecret" placeholder="至少 8 位" />
      </a-form-item>

      <a-form-item label="Redirect URIs">
        <a-select
          v-model:value="form.redirectUris"
          mode="tags"
          placeholder="http://localhost:3000/callback"
          :token-separators="[',', ' ']"
        />
      </a-form-item>

      <a-form-item label="Scopes">
        <a-select v-model:value="form.scopes" mode="multiple" :options="defaultScopes.map(s => ({ label: s, value: s }))" />
      </a-form-item>

      <a-form-item label="授权类型">
        <a-select
          v-model:value="form.authorizationGrantTypes"
          mode="multiple"
          :options="defaultGrants.map(g => ({ label: g, value: g }))"
        />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="强制 PKCE">
            <a-switch v-model:checked="form.requirePkce" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="授权同意页">
            <a-switch v-model:checked="form.requireConsent" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="Access Token TTL (秒)">
            <a-input-number v-model:value="form.accessTokenTtl" :min="60" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="Refresh Token TTL (秒)">
        <a-input-number v-model:value="form.refreshTokenTtl" :min="60" style="width: 100%" />
      </a-form-item>

      <a-form-item label="Back-Channel Logout URI">
        <a-input v-model:value="form.backchannelLogoutUri" placeholder="http://localhost:8080/auth/backchannel-logout" />
      </a-form-item>
      <a-form-item label="Front-Channel Logout URI">
        <a-input v-model:value="form.frontchannelLogoutUri" placeholder="http://localhost:8080/auth/frontchannel-logout" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="secretModalOpen"
    title="请妥善保存 Client Secret"
    :footer="null"
    :closable="true"
  >
    <a-alert
      type="warning"
      show-icon
      message="密钥仅显示一次，关闭后无法再次查看。请立即复制保存。"
      style="margin-bottom: 16px"
    />
    <a-typography-paragraph copyable :content="revealedSecret" />
  </a-modal>
</template>
