<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { tenantApi } from '@/api/admin'
import type { TenantInfo } from '@/types'

const loading = ref(false)
const tenants = ref<TenantInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const modalOpen = ref(false)
const editing = ref<TenantInfo | null>(null)

const form = reactive({
  code: '',
  name: '',
})

async function loadTenants() {
  loading.value = true
  try {
    const result = await tenantApi.list(page.value, pageSize.value)
    tenants.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.code = ''
  form.name = ''
  modalOpen.value = true
}

function openEdit(record: TenantInfo) {
  editing.value = record
  form.code = record.code
  form.name = record.name
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await tenantApi.update(editing.value.id, { name: form.name })
      message.success('租户已更新')
    } else {
      await tenantApi.create({ code: form.code, name: form.name })
      message.success('租户已创建')
    }
    modalOpen.value = false
    await loadTenants()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleStatus(record: TenantInfo) {
  try {
    if (record.status === 1) {
      await tenantApi.disable(record.id)
      message.success('租户已禁用')
    } else {
      await tenantApi.enable(record.id)
      message.success('租户已启用')
    }
    await loadTenants()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function confirmDelete(record: TenantInfo) {
  Modal.confirm({
    title: '删除租户',
    content: `确定删除租户 ${record.name}？`,
    okType: 'danger',
    onOk: async () => {
      await tenantApi.remove(record.id)
      message.success('租户已删除')
      await loadTenants()
    },
  })
}

onMounted(loadTenants)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-typography-title :level="4" style="margin: 0">租户管理</a-typography-title>
      <a-button type="primary" @click="openCreate">创建租户</a-button>
    </div>

    <a-table
      :data-source="tenants"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page,
        pageSize,
        total,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadTenants() },
      }"
    >
      <a-table-column title="编码" data-index="code" key="code" />
      <a-table-column title="名称" data-index="name" key="name" />
      <a-table-column title="状态" key="status" width="100">
        <template #default="{ record }">
          <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '正常' : '禁用'" />
        </template>
      </a-table-column>
      <a-table-column title="类型" key="system" width="100">
        <template #default="{ record }">
          <a-tag v-if="record.system" color="blue">系统</a-tag>
          <span v-else>自定义</span>
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="220">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" :disabled="record.system" @click="toggleStatus(record)">
              {{ record.status === 1 ? '禁用' : '启用' }}
            </a-button>
            <a-button type="link" danger size="small" :disabled="record.system" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑租户' : '创建租户'" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-form-item label="租户编码" required>
        <a-input v-model:value="form.code" :disabled="!!editing" placeholder="acme-corp" />
      </a-form-item>
      <a-form-item label="租户名称" required>
        <a-input v-model:value="form.name" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
