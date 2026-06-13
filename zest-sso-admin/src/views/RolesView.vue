<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { roleApi } from '@/api/admin'
import type { RoleInfo } from '@/types'

const loading = ref(false)
const roles = ref<RoleInfo[]>([])
const modalOpen = ref(false)
const editing = ref<RoleInfo | null>(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
})

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await roleApi.list()
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
  form.description = ''
  modalOpen.value = true
}

function openEdit(record: RoleInfo) {
  editing.value = record
  form.code = record.code
  form.name = record.name
  form.description = record.description || ''
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await roleApi.update(editing.value.id, { name: form.name, description: form.description })
      message.success('角色已更新')
    } else {
      await roleApi.create({ code: form.code, name: form.name, description: form.description })
      message.success('角色已创建')
    }
    modalOpen.value = false
    await loadRoles()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function confirmDelete(record: RoleInfo) {
  Modal.confirm({
    title: '删除角色',
    content: `确定删除角色 ${record.name}？`,
    okType: 'danger',
    onOk: async () => {
      await roleApi.remove(record.id)
      message.success('角色已删除')
      await loadRoles()
    },
  })
}

onMounted(loadRoles)
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-typography-title :level="4" style="margin: 0">角色管理</a-typography-title>
      <a-button type="primary" @click="openCreate">创建角色</a-button>
    </div>

    <a-table :data-source="roles" :loading="loading" row-key="id" :pagination="false">
      <a-table-column title="编码" data-index="code" key="code" />
      <a-table-column title="名称" data-index="name" key="name" />
      <a-table-column title="描述" data-index="description" key="description" />
      <a-table-column title="类型" key="system" width="100">
        <template #default="{ record }">
          <a-tag :color="record.system ? 'blue' : 'default'">{{ record.system ? '系统' : '自定义' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="160">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" :disabled="record.system" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" danger size="small" :disabled="record.system" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑角色' : '创建角色'" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-form-item label="角色编码" required>
        <a-input v-model:value="form.code" :disabled="!!editing" placeholder="CUSTOM_ROLE" />
      </a-form-item>
      <a-form-item label="角色名称" required>
        <a-input v-model:value="form.name" />
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="form.description" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
