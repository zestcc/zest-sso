<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { groupApi, roleApi } from '@/api/admin'
import type { GroupInfo, RoleInfo } from '@/types'

const loading = ref(false)
const groups = ref<GroupInfo[]>([])
const roles = ref<RoleInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const modalOpen = ref(false)
const editing = ref<GroupInfo | null>(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
  roleCodes: [] as string[],
})

async function loadGroups() {
  loading.value = true
  try {
    const result = await groupApi.list(page.value, pageSize.value, keyword.value || undefined)
    groups.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editing.value = null
  form.code = ''
  form.name = ''
  form.description = ''
  form.roleCodes = []
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: GroupInfo) {
  editing.value = record
  form.code = record.code
  form.name = record.name
  form.description = record.description || ''
  form.roleCodes = [...(record.roleCodes || [])]
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      await groupApi.update(editing.value.id, {
        name: form.name,
        description: form.description,
        roleCodes: form.roleCodes,
      })
      message.success('用户组已更新')
    } else {
      await groupApi.create({
        code: form.code,
        name: form.name,
        description: form.description,
        roleCodes: form.roleCodes,
      })
      message.success('用户组已创建')
    }
    modalOpen.value = false
    await loadGroups()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function confirmDelete(record: GroupInfo) {
  Modal.confirm({
    title: '删除用户组',
    content: `确定删除用户组 ${record.name}？`,
    okType: 'danger',
    onOk: async () => {
      await groupApi.remove(record.id)
      message.success('用户组已删除')
      await loadGroups()
    },
  })
}

onMounted(async () => {
  roles.value = await roleApi.list()
  await loadGroups()
})
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索编码/名称"
        style="width: 280px"
        @search="() => { page = 1; loadGroups() }"
      />
      <a-button type="primary" @click="openCreate">创建用户组</a-button>
    </div>

    <a-table
      :data-source="groups"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page,
        pageSize,
        total,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadGroups() },
      }"
    >
      <a-table-column title="编码" data-index="code" key="code" />
      <a-table-column title="名称" data-index="name" key="name" />
      <a-table-column title="角色" key="roles">
        <template #default="{ record }">
          <a-tag v-for="role in record.roleCodes" :key="role">{{ role }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="成员数" data-index="memberCount" key="memberCount" width="90" />
      <a-table-column title="操作" key="actions" width="160">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" danger size="small" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑用户组' : '创建用户组'" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-form-item label="编码" required>
        <a-input v-model:value="form.code" :disabled="!!editing" />
      </a-form-item>
      <a-form-item label="名称" required>
        <a-input v-model:value="form.name" />
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="form.description" />
      </a-form-item>
      <a-form-item label="关联角色">
        <a-select
          v-model:value="form.roleCodes"
          mode="multiple"
          :options="roles.map(r => ({ label: `${r.name} (${r.code})`, value: r.code }))"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
