<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { groupApi, roleApi, tenantApi, userApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { isSsoAdmin } from '@/utils/permissions'
import type { GroupInfo, RoleInfo, TenantInfo, UpdateUserPayload, UserInfo } from '@/types'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<UserInfo[]>([])
const roles = ref<RoleInfo[]>([])
const groups = ref<GroupInfo[]>([])
const tenants = ref<TenantInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const modalOpen = ref(false)
const passwordModalOpen = ref(false)
const detailOpen = ref(false)
const editing = ref<UserInfo | null>(null)
const detailUser = ref<UserInfo | null>(null)
const passwordUserId = ref<number | null>(null)
const newPassword = ref('')

const assignableRoles = computed(() => {
  if (isSsoAdmin(auth.user)) return roles.value
  return roles.value.filter((r) => r.code === 'USER')
})

const form = reactive({
  username: '',
  email: '',
  password: '',
  displayName: '',
  roleCodes: ['USER'] as string[],
  groupIds: [] as number[],
  tenantIds: [] as number[],
  defaultTenantId: undefined as number | undefined,
})

const statusMap: Record<number, { text: string; color: string }> = {
  1: { text: '正常', color: 'success' },
  0: { text: '禁用', color: 'default' },
  2: { text: '锁定', color: 'error' },
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await userApi.list(page.value, pageSize.value, keyword.value || undefined)
    users.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  const [roleList, groupList, tenantResult] = await Promise.all([
    roleApi.list(),
    groupApi.listAll(),
    tenantApi.list(1, 100),
  ])
  roles.value = roleList
  groups.value = groupList
  tenants.value = tenantResult.records
}

function resetForm() {
  editing.value = null
  form.username = ''
  form.email = ''
  form.password = ''
  form.displayName = ''
  form.roleCodes = ['USER']
  form.groupIds = []
  form.tenantIds = tenants.value.length ? [tenants.value[0].id] : []
  form.defaultTenantId = tenants.value[0]?.id
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: UserInfo) {
  editing.value = record
  form.username = record.username
  form.email = record.email
  form.displayName = record.displayName
  form.roleCodes = [...record.roles]
  form.groupIds = record.groupIds ? [...record.groupIds] : []
  form.tenantIds = record.tenants.map((t) => t.id)
  form.defaultTenantId = record.defaultTenantId ?? undefined
  modalOpen.value = true
}

async function openDetail(record: UserInfo) {
  detailUser.value = await userApi.get(record.id)
  detailOpen.value = true
}

async function handleSubmit() {
  try {
    if (editing.value) {
      const payload: UpdateUserPayload = {
        email: form.email,
        displayName: form.displayName,
        roleCodes: form.roleCodes,
        groupIds: form.groupIds,
        tenantIds: form.tenantIds,
        defaultTenantId: form.defaultTenantId,
      }
      await userApi.update(editing.value.id, payload)
      message.success('用户已更新')
    } else {
      await userApi.create({
        username: form.username,
        email: form.email,
        password: form.password,
        displayName: form.displayName,
        roleCodes: form.roleCodes,
        groupIds: form.groupIds,
        tenantIds: form.tenantIds,
        defaultTenantId: form.defaultTenantId,
      })
      message.success('用户已创建')
    }
    modalOpen.value = false
    await loadUsers()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleStatus(record: UserInfo) {
  try {
    if (record.status === 2) {
      await userApi.unlock(record.id)
      message.success('用户已解锁')
    } else if (record.status === 1) {
      await userApi.disable(record.id)
      message.success('用户已禁用')
    } else {
      await userApi.enable(record.id)
      message.success('用户已启用')
    }
    await loadUsers()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function openResetPassword(record: UserInfo) {
  passwordUserId.value = record.id
  newPassword.value = ''
  passwordModalOpen.value = true
}

async function submitResetPassword() {
  if (!passwordUserId.value || newPassword.value.length < 8) {
    message.error('新密码至少 8 位')
    return
  }
  try {
    await userApi.resetPassword(passwordUserId.value, newPassword.value)
    message.success('密码已重置')
    passwordModalOpen.value = false
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

function confirmResetMfa(record: UserInfo) {
  Modal.confirm({
    title: '重置 MFA',
    content: `确定重置用户 ${record.username} 的多因素认证？重置后用户需重新绑定验证器。`,
    okType: 'danger',
    onOk: async () => {
      await userApi.resetMfa(record.id)
      message.success('MFA 已重置')
      await loadUsers()
    },
  })
}

function confirmDelete(record: UserInfo) {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除用户 ${record.username}？`,
    okType: 'danger',
    onOk: async () => {
      await userApi.remove(record.id)
      message.success('用户已删除')
      await loadUsers()
    },
  })
}

function statusActionText(status: number) {
  if (status === 2) return '解锁'
  if (status === 1) return '禁用'
  return '启用'
}

onMounted(async () => {
  await loadMeta()
  await loadUsers()
})
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索用户名/邮箱/显示名"
        style="width: 280px"
        @search="() => { page = 1; loadUsers() }"
      />
      <a-button type="primary" @click="openCreate">创建用户</a-button>
    </div>

    <a-table
      :data-source="users"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: page,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p: number, s: number) => { page = p; pageSize = s; loadUsers() },
      }"
    >
      <a-table-column title="用户名" data-index="username" key="username" />
      <a-table-column title="显示名" data-index="displayName" key="displayName" />
      <a-table-column title="邮箱" data-index="email" key="email" />
      <a-table-column title="角色" key="roles">
        <template #default="{ record }">
          <a-tag v-for="role in record.roles" :key="role">{{ role }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="status" width="90">
        <template #default="{ record }">
          <a-badge
            :status="statusMap[record.status]?.color || 'default'"
            :text="statusMap[record.status]?.text || '未知'"
          />
        </template>
      </a-table-column>
      <a-table-column title="MFA" key="mfa" width="80">
        <template #default="{ record }">
          <a-tag :color="record.mfaEnabled ? 'green' : 'default'">{{ record.mfaEnabled ? '已启用' : '未启用' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="360">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="toggleStatus(record)">{{ statusActionText(record.status) }}</a-button>
            <a-button type="link" size="small" @click="openResetPassword(record)">重置密码</a-button>
            <a-button
              v-if="isSsoAdmin(auth.user) && record.mfaEnabled"
              type="link"
              size="small"
              danger
              @click="confirmResetMfa(record)"
            >重置 MFA</a-button>
            <a-button type="link" danger size="small" :disabled="record.username === 'admin'" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑用户' : '创建用户'" width="640px" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="用户名" required>
            <a-input v-model:value="form.username" :disabled="!!editing" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="显示名">
            <a-input v-model:value="form.displayName" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="邮箱">
        <a-input v-model:value="form.email" />
      </a-form-item>
      <a-form-item v-if="!editing" label="密码" required>
        <a-input-password v-model:value="form.password" placeholder="至少 8 位" />
      </a-form-item>
      <a-form-item label="角色">
        <a-select
          v-model:value="form.roleCodes"
          mode="multiple"
          :options="assignableRoles.map(r => ({ label: `${r.name} (${r.code})`, value: r.code }))"
        />
      </a-form-item>
      <a-form-item label="用户组">
        <a-select
          v-model:value="form.groupIds"
          mode="multiple"
          :options="groups.map(g => ({ label: `${g.name} (${g.code})`, value: g.id }))"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="租户">
        <a-select
          v-model:value="form.tenantIds"
          mode="multiple"
          :options="tenants.map(t => ({ label: t.name, value: t.id }))"
        />
      </a-form-item>
      <a-form-item label="默认租户">
        <a-select
          v-model:value="form.defaultTenantId"
          :options="tenants.filter(t => form.tenantIds.includes(t.id)).map(t => ({ label: t.name, value: t.id }))"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-drawer v-model:open="detailOpen" title="用户详情" width="480">
    <a-descriptions v-if="detailUser" bordered :column="1">
      <a-descriptions-item label="用户名">{{ detailUser.username }}</a-descriptions-item>
      <a-descriptions-item label="显示名">{{ detailUser.displayName }}</a-descriptions-item>
      <a-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</a-descriptions-item>
      <a-descriptions-item label="角色">{{ detailUser.roles.join(', ') }}</a-descriptions-item>
      <a-descriptions-item label="用户组">{{ detailUser.groups?.join(', ') || '-' }}</a-descriptions-item>
      <a-descriptions-item label="租户">{{ detailUser.tenants.map(t => t.name).join(', ') }}</a-descriptions-item>
      <a-descriptions-item label="MFA">
        <a-tag :color="detailUser.mfaEnabled ? 'green' : 'default'">{{ detailUser.mfaEnabled ? '已启用' : '未启用' }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="最后登录">
        {{ detailUser.lastLoginAt ? dayjs(detailUser.lastLoginAt).format('YYYY-MM-DD HH:mm:ss') : '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="最后登录 IP">{{ detailUser.lastLoginIp || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-drawer>

  <a-modal v-model:open="passwordModalOpen" title="重置密码" @ok="submitResetPassword">
    <a-input-password v-model:value="newPassword" placeholder="新密码至少 8 位" />
  </a-modal>
</template>
