<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { identityProviderApi } from '@/api/admin'
import type { FederatedIdpAdapterInfo, IdentityProviderInfo } from '@/types'

const loading = ref(false)
const providers = ref<IdentityProviderInfo[]>([])
const adapters = ref<FederatedIdpAdapterInfo[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const modalOpen = ref(false)
const editing = ref<IdentityProviderInfo | null>(null)

const form = reactive({
  alias: '',
  displayName: '',
  providerType: 'OIDC' as 'OIDC' | 'SAML',
  adapterKey: 'generic-oidc',
  discoveryUri: '',
  clientId: '',
  clientSecret: '',
  scopes: 'openid,profile,email',
  usernameClaim: '',
  emailClaim: '',
  displayNameClaim: '',
  roleClaim: '',
  defaultRoleCodes: 'USER',
  samlMetadataUri: '',
  samlEntityId: '',
  samlSsoUrl: '',
  samlVerificationCertificate: '',
})

function resetForm() {
  Object.assign(form, {
    alias: '',
    displayName: '',
    providerType: 'OIDC',
    adapterKey: 'generic-oidc',
    discoveryUri: '',
    clientId: '',
    clientSecret: '',
    scopes: 'openid,profile,email',
    usernameClaim: '',
    emailClaim: '',
    displayNameClaim: '',
    roleClaim: '',
    defaultRoleCodes: 'USER',
    samlMetadataUri: '',
    samlEntityId: '',
    samlSsoUrl: '',
    samlVerificationCertificate: '',
  })
}

async function loadAdapters() {
  try {
    adapters.value = await identityProviderApi.listAdapters()
  } catch {
    adapters.value = []
  }
}

function applyAdapterDefaults(key: string) {
  const adapter = adapters.value.find((a) => a.key === key)
  if (!adapter) return
  if (adapter.defaultEndpoints?.discoveryUri) {
    form.discoveryUri = adapter.defaultEndpoints.discoveryUri
  }
  if (adapter.defaultClaims?.usernameClaim) form.usernameClaim = adapter.defaultClaims.usernameClaim
  if (adapter.defaultClaims?.emailClaim) form.emailClaim = adapter.defaultClaims.emailClaim
  if (adapter.defaultClaims?.displayNameClaim) form.displayNameClaim = adapter.defaultClaims.displayNameClaim
  if (key === 'feishu' && !form.scopes) form.scopes = 'openid,profile,email'
  if (key === 'dingtalk' && !form.scopes) form.scopes = 'openid,profile'
  if (key === 'wecom' && !form.scopes) form.scopes = 'snsapi_privateinfo'
}

async function loadProviders() {
  loading.value = true
  try {
    const result = await identityProviderApi.list(page.value, pageSize.value)
    providers.value = result.records
    total.value = result.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: IdentityProviderInfo) {
  editing.value = record
  Object.assign(form, {
    alias: record.alias,
    displayName: record.displayName,
    providerType: (record.providerType === 'SAML' ? 'SAML' : 'OIDC') as 'OIDC' | 'SAML',
    adapterKey: record.adapterKey || 'generic-oidc',
    discoveryUri: record.discoveryUri || '',
    clientId: record.clientId || '',
    clientSecret: '',
    scopes: record.scopes || 'openid,profile,email',
    usernameClaim: record.usernameClaim || '',
    emailClaim: record.emailClaim || '',
    displayNameClaim: record.displayNameClaim || '',
    roleClaim: record.roleClaim || '',
    defaultRoleCodes: record.defaultRoleCodes || 'USER',
    samlMetadataUri: record.samlMetadataUri || '',
    samlSsoUrl: record.samlSsoUrl || '',
    samlVerificationCertificate: '',
  })
  modalOpen.value = true
}

async function importSamlMetadata() {
  if (!form.samlMetadataUri) {
    message.warning('请先填写元数据 URL')
    return
  }
  try {
    const metadata = await identityProviderApi.parseSamlMetadata(form.samlMetadataUri)
    form.samlEntityId = metadata.entityId
    form.samlSsoUrl = metadata.ssoUrl
    form.samlVerificationCertificate = metadata.verificationCertificate
    message.success('元数据已导入')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '导入失败')
  }
}

async function handleSubmit() {
  try {
    const claimPayload = {
      usernameClaim: form.usernameClaim || undefined,
      emailClaim: form.emailClaim || undefined,
      displayNameClaim: form.displayNameClaim || undefined,
      roleClaim: form.roleClaim || undefined,
      defaultRoleCodes: form.defaultRoleCodes || undefined,
    }
    if (editing.value?.id) {
      await identityProviderApi.update(editing.value.id, {
        displayName: form.displayName,
        ...claimPayload,
        ...(form.providerType === 'SAML'
          ? {
              samlMetadataUri: form.samlMetadataUri || undefined,
              samlEntityId: form.samlEntityId || undefined,
              samlSsoUrl: form.samlSsoUrl || undefined,
              samlVerificationCertificate: form.samlVerificationCertificate || undefined,
            }
          : {
              discoveryUri: form.discoveryUri || undefined,
              clientId: form.clientId || undefined,
              clientSecret: form.clientSecret || undefined,
              scopes: form.scopes,
            }),
      })
      message.success('身份源已更新')
    } else {
      await identityProviderApi.create({
        alias: form.alias,
        displayName: form.displayName,
        providerType: form.providerType,
        ...claimPayload,
        ...(form.providerType === 'SAML'
          ? {
              samlMetadataUri: form.samlMetadataUri || undefined,
              samlEntityId: form.samlEntityId,
              samlSsoUrl: form.samlSsoUrl,
              samlVerificationCertificate: form.samlVerificationCertificate,
            }
          : {
              adapterKey: form.adapterKey,
              discoveryUri: form.discoveryUri || undefined,
              clientId: form.clientId,
              clientSecret: form.clientSecret,
              scopes: form.scopes,
            }),
      })
      message.success('身份源已创建')
    }
    modalOpen.value = false
    await loadProviders()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleEnabled(record: IdentityProviderInfo) {
  if (!record.id) return
  await identityProviderApi.update(record.id, { enabled: record.enabled === 1 ? 0 : 1 })
  message.success('状态已更新')
  await loadProviders()
}

function confirmDelete(record: IdentityProviderInfo) {
  Modal.confirm({
    title: '删除身份源',
    content: `确定删除 ${record.displayName}？`,
    okType: 'danger',
    onOk: async () => {
      await identityProviderApi.remove(record.id!)
      message.success('已删除')
      await loadProviders()
    },
  })
}

onMounted(async () => {
  await loadAdapters()
  await loadProviders()
})
</script>

<template>
  <div class="page-card">
    <div class="page-toolbar">
      <a-typography-title :level="4" style="margin: 0">身份联邦 (IdP)</a-typography-title>
      <a-button type="primary" @click="openCreate">添加身份源</a-button>
    </div>
    <a-alert
      type="info"
      show-icon
      message="支持 OIDC 与 SAML 2.0 联邦。OIDC 登录地址为 /oauth2/authorization/{alias}，SAML 为 /saml2/authenticate/{alias}。"
      style="margin-bottom: 16px"
    />
    <a-table
      :data-source="providers"
      :loading="loading"
      row-key="id"
      :pagination="{ current: page, pageSize, total, onChange: (p: number, s: number) => { page = p; pageSize = s; loadProviders() } }"
    >
      <a-table-column title="Alias" data-index="alias" key="alias" />
      <a-table-column title="名称" data-index="displayName" key="displayName" />
      <a-table-column title="适配器" data-index="adapterKey" key="adapterKey" width="110" />
      <a-table-column title="类型" data-index="providerType" key="providerType" width="80" />
      <a-table-column title="登录地址" key="loginUrl">
        <template #default="{ record }">
          <a-typography-text code copyable>{{ record.loginUrl }}</a-typography-text>
        </template>
      </a-table-column>
      <a-table-column title="状态" key="enabled" width="90">
        <template #default="{ record }">
          <a-badge :status="record.enabled === 1 ? 'success' : 'default'" :text="record.enabled === 1 ? '启用' : '禁用'" />
        </template>
      </a-table-column>
      <a-table-column title="操作" key="actions" width="200">
        <template #default="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="toggleEnabled(record)">{{ record.enabled === 1 ? '禁用' : '启用' }}</a-button>
            <a-button type="link" danger size="small" @click="confirmDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </div>

  <a-modal v-model:open="modalOpen" :title="editing ? '编辑身份源' : '添加身份源'" width="760px" @ok="handleSubmit">
    <a-form layout="vertical">
      <a-form-item label="Alias" required>
        <a-input v-model:value="form.alias" :disabled="!!editing" placeholder="okta / azure-ad" />
      </a-form-item>
      <a-form-item label="显示名称" required>
        <a-input v-model:value="form.displayName" />
      </a-form-item>
      <a-form-item label="协议类型" required>
        <a-radio-group v-model:value="form.providerType" :disabled="!!editing">
          <a-radio-button value="OIDC">OIDC</a-radio-button>
          <a-radio-button value="SAML">SAML 2.0</a-radio-button>
        </a-radio-group>
      </a-form-item>

      <template v-if="form.providerType === 'OIDC'">
        <a-form-item label="平台适配器">
          <a-select v-model:value="form.adapterKey" :disabled="!!editing" @change="(v: string) => applyAdapterDefaults(v)">
            <a-select-option v-for="a in adapters" :key="a.key" :value="a.key">
              {{ a.displayName }}
              <span v-if="!a.productionReady">（预览）</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="OIDC Discovery URI">
          <a-input v-model:value="form.discoveryUri" placeholder="飞书/钉钉可留空由适配器填充" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Client ID" required>
              <a-input v-model:value="form.clientId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="editing ? 'Client Secret（留空不修改）' : 'Client Secret'" :required="!editing">
              <a-input-password v-model:value="form.clientSecret" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="Scopes">
          <a-input v-model:value="form.scopes" />
        </a-form-item>
      </template>

      <template v-else>
        <a-form-item label="元数据 URL（可选，一键导入）">
          <a-input-group compact>
            <a-input v-model:value="form.samlMetadataUri" placeholder="https://idp.example.com/metadata.xml" style="width: calc(100% - 100px)" />
            <a-button type="primary" @click="importSamlMetadata">导入</a-button>
          </a-input-group>
        </a-form-item>
        <a-form-item label="IdP Entity ID" required>
          <a-input v-model:value="form.samlEntityId" placeholder="https://idp.example.com/metadata" />
        </a-form-item>
        <a-form-item label="SSO URL" required>
          <a-input v-model:value="form.samlSsoUrl" placeholder="https://idp.example.com/sso/saml" />
        </a-form-item>
        <a-form-item :label="editing ? '验证证书 PEM（留空不修改）' : '验证证书 PEM'" :required="!editing">
          <a-textarea v-model:value="form.samlVerificationCertificate" :rows="5" placeholder="-----BEGIN CERTIFICATE-----..." />
        </a-form-item>
      </template>

      <a-divider>属性映射</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="用户名 Claim">
            <a-input v-model:value="form.usernameClaim" :placeholder="form.providerType === 'SAML' ? 'uid' : 'preferred_username'" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱 Claim">
            <a-input v-model:value="form.emailClaim" placeholder="email" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="显示名 Claim">
            <a-input v-model:value="form.displayNameClaim" :placeholder="form.providerType === 'SAML' ? 'displayName' : 'name'" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="角色 Claim">
            <a-input v-model:value="form.roleClaim" placeholder="groups / roles（可选）" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="默认角色（逗号分隔）">
        <a-input v-model:value="form.defaultRoleCodes" placeholder="USER" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
