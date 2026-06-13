<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { ldapApi } from '@/api/admin'
import type { LdapProviderInfo } from '@/types'

const loading = ref(false)
const providers = ref<LdapProviderInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const modalOpen = ref(false)
const editing = ref<LdapProviderInfo | null>(null)

const form = reactive({
  alias: '',
  displayName: '',
  serverUrl: 'ldap://localhost:389',
  baseDn: 'dc=example,dc=com',
  bindDn: '',
  bindPassword: '',
  userSearchBase: 'ou=users,dc=example,dc=com',
  userSearchFilter: '(uid={0})',
  groupSearchBase: '',
  groupRoleAttribute: 'cn',
})

async function loadProviders() {
  loading.value = true
  try {
    const result = await ldapApi.list(page.value, pageSize.value)
    providers.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editing.value = null
  Object.assign(form, {
    alias: '',
    displayName: '',
    serverUrl: 'ldap://localhost:389',
    baseDn: 'dc=example,dc=com',
    bindDn: '',
    bindPassword: '',
    userSearchBase: 'ou=users,dc=example,dc=com',
    userSearchFilter: '(uid={0})',
    groupSearchBase: '',
    groupRoleAttribute: 'cn',
  })
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: LdapProviderInfo) {
  editing.value = record
  Object.assign(form, {
    alias: record.alias,
    displayName: record.displayName,
    serverUrl: record.serverUrl,
    baseDn: record.baseDn,
    bindDn: record.bindDn || '',
    bindPassword: '',
    userSearchBase: record.userSearchBase,
    userSearchFilter: record.userSearchFilter || '(uid={0})',
    groupSearchBase: record.groupSearchBase || '',
    groupRoleAttribute: record.groupRoleAttribute || 'cn',
  })
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value?.id) {
      await ldapApi.update(editing.value.id, { ...form, enabled: editing.value.enabled })
      message.success('LDAP 配置已更新')
    } else {
      await ldapApi.create(form)
      message.success('LDAP 配置已创建')
    }
    modalOpen.value = false
    await loadProviders()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function testConnection(record: LdapProviderInfo) {
  try {
    await ldapApi.test(record.id!)
    message.success('连接测试成功')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '连接失败')
  }
}

function confirmDelete(record: LdapProviderInfo) {
  Modal.confirm({
    title: '删除 LDAP 配置',
    content: `确定删除 ${record.displayName}？`,
    okType: 'danger',
    onOk: async () => {
      await ldapApi.remove(record.id!)
      message.success('已删除')
      await loadProviders()
    },
  })
}

onMounted(loadProviders)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-typography-text type="secondary">使用 Spring Security LDAP 标准认证，与本地账号并存。</a-typography-text>
      <a-button type="primary" @click="openCreate">添加 LDAP</a-button>
    </div>

    <a-table
      :data-source="providers"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page,
        pageSize,
        total,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadProviders() },
      }"
    >
      <a-table-column title="别名" data-index="alias" key="alias" />
      <a-table-column title="名称" data-index="displayName" key="displayName" />
      <a-table-column title="服务器" data-index="serverUrl" key="serverUrl" />
      <a-table-column title="状态" key="enabled" width="90">
        <template #default="{ record }">
          <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="220">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="testConnection(record)">测试连接</a-button>
            <a-button type="link" danger size="small" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑 LDAP' : '添加 LDAP'" width="720px" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="别名" required><a-input v-model:value="form.alias" :disabled="!!editing" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="显示名称" required><a-input v-model:value="form.displayName" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="LDAP URL" required><a-input v-model:value="form.serverUrl" placeholder="ldap://host:389" /></a-form-item>
      <a-form-item label="Base DN" required><a-input v-model:value="form.baseDn" /></a-form-item>
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="Bind DN"><a-input v-model:value="form.bindDn" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="Bind 密码"><a-input-password v-model:value="form.bindPassword" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="用户搜索 Base" required><a-input v-model:value="form.userSearchBase" /></a-form-item>
      <a-form-item label="用户搜索 Filter"><a-input v-model:value="form.userSearchFilter" /></a-form-item>
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="组搜索 Base"><a-input v-model:value="form.groupSearchBase" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="组角色属性"><a-input v-model:value="form.groupRoleAttribute" /></a-form-item></a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>
