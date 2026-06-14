<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { authApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import type { MfaSetupInfo, WebauthnCredentialInfo } from '@/types'
import { credentialToJson, formatWebAuthnError, isWebAuthnSupported, parseRegistrationOptions } from '@/utils/webauthn'

const auth = useAuthStore()
const loading = ref(false)
const mfaSetup = ref<MfaSetupInfo | null>(null)
const mfaCode = ref('')
const passkeys = ref<WebauthnCredentialInfo[]>([])
const passkeyNickname = ref('我的 Passkey')
const passkeyLoading = ref(false)
const passkeySupported = isWebAuthnSupported()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

async function loadMfaSetup() {
  mfaSetup.value = await authApi.mfaSetup()
}

async function loadPasskeys() {
  if (!passkeySupported) return
  passkeys.value = await authApi.webauthnList()
}

async function registerPasskey() {
  passkeyLoading.value = true
  try {
    const options = await authApi.webauthnRegisterOptions(passkeyNickname.value)
    const credential = await navigator.credentials.create({
      publicKey: parseRegistrationOptions(options.publicKey),
    })
    if (!credential) throw new Error('未创建 Passkey')
    await authApi.webauthnRegisterFinish(
      options.sessionToken,
      credentialToJson(credential),
      passkeyNickname.value,
    )
    message.success('Passkey 已注册')
    await loadPasskeys()
  } catch (e) {
    message.error(formatWebAuthnError(e))
  } finally {
    passkeyLoading.value = false
  }
}

async function deletePasskey(id: number) {
  await authApi.webauthnDelete(id)
  message.success('Passkey 已删除')
  await loadPasskeys()
}

async function handleChangePassword() {
  if (form.newPassword.length < 8) {
    message.error('新密码至少 8 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }
  loading.value = true
  try {
    await authApi.changePassword(form.currentPassword, form.newPassword)
    message.success('密码修改成功，请重新登录')
    await auth.logout()
    window.location.href = `${import.meta.env.BASE_URL}login`
  } catch (e) {
    message.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    loading.value = false
  }
}

async function enableMfa() {
  if (!mfaCode.value) return
  await authApi.mfaEnable(mfaCode.value)
  message.success('MFA 已启用')
  mfaCode.value = ''
  await loadMfaSetup()
  await auth.fetchMe()
}

async function disableMfa() {
  if (!mfaCode.value) return
  await authApi.mfaDisable(mfaCode.value)
  message.success('MFA 已禁用')
  mfaCode.value = ''
  await loadMfaSetup()
  await auth.fetchMe()
}

onMounted(async () => {
  await loadMfaSetup()
  await loadPasskeys()
})
</script>

<template>
  <a-row :gutter="16">
    <a-col :xs="24" :lg="12">
      <div class="page-card">
        <a-typography-title :level="4">账号信息</a-typography-title>
        <a-descriptions bordered :column="1" style="margin-top: 16px">
          <a-descriptions-item label="用户名">{{ auth.user?.username }}</a-descriptions-item>
          <a-descriptions-item label="显示名">{{ auth.user?.displayName }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ auth.user?.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="角色">{{ auth.user?.roles?.join(', ') }}</a-descriptions-item>
          <a-descriptions-item label="MFA">
            <a-tag :color="auth.user?.mfaEnabled ? 'green' : 'default'">{{ auth.user?.mfaEnabled ? '已启用' : '未启用' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="最后登录">
            {{ auth.user?.lastLoginAt ? dayjs(auth.user.lastLoginAt).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-col>
    <a-col :xs="24" :lg="12">
      <div class="page-card">
        <a-typography-title :level="4">修改密码</a-typography-title>
        <a-form layout="vertical" style="margin-top: 16px" @finish="handleChangePassword">
          <a-form-item label="当前密码" required>
            <a-input-password v-model:value="form.currentPassword" />
          </a-form-item>
          <a-form-item label="新密码" required>
            <a-input-password v-model:value="form.newPassword" />
          </a-form-item>
          <a-form-item label="确认新密码" required>
            <a-input-password v-model:value="form.confirmPassword" />
          </a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">保存新密码</a-button>
        </a-form>
      </div>
    </a-col>
    <a-col :span="24" style="margin-top: 16px">
      <div class="page-card">
        <a-typography-title :level="4">多因素认证 (MFA)</a-typography-title>
        <template v-if="mfaSetup">
          <a-alert
            v-if="!mfaSetup.enabled"
            type="info"
            show-icon
            message="使用 Google Authenticator 等 TOTP 应用扫描二维码或手动输入密钥后，输入验证码完成绑定。"
            style="margin: 16px 0"
          />
          <a-descriptions v-if="!mfaSetup.enabled" bordered :column="1" style="margin-bottom: 16px">
            <a-descriptions-item label="密钥">
              <a-typography-text copyable>{{ mfaSetup.secret }}</a-typography-text>
            </a-descriptions-item>
            <a-descriptions-item label="OTP Auth URL">
              <a-typography-text copyable :content="mfaSetup.otpAuthUrl" />
            </a-descriptions-item>
          </a-descriptions>
          <a-input v-model:value="mfaCode" placeholder="6 位验证码" style="width: 200px; margin-right: 12px" maxlength="6" />
          <a-button v-if="!mfaSetup.enabled" type="primary" @click="enableMfa">启用 MFA</a-button>
          <a-button v-else danger @click="disableMfa">禁用 MFA</a-button>
        </template>
      </div>
    </a-col>
    <a-col v-if="passkeySupported" :span="24" style="margin-top: 16px">
      <div class="page-card">
        <a-typography-title :level="4">Passkey（WebAuthn）</a-typography-title>
        <a-alert
          type="info"
          show-icon
          message="Passkey 可替代密码登录，支持 Windows Hello、Touch ID、安全密钥等。"
          style="margin: 16px 0"
        />
        <a-space style="margin-bottom: 16px">
          <a-input v-model:value="passkeyNickname" placeholder="Passkey 名称" style="width: 200px" />
          <a-button type="primary" :loading="passkeyLoading" @click="registerPasskey">注册新 Passkey</a-button>
        </a-space>
        <a-table :data-source="passkeys" row-key="id" :pagination="false" size="small">
          <a-table-column title="名称" data-index="nickname" key="nickname" />
          <a-table-column title="注册时间" key="createTime">
            <template #default="{ record }">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}</template>
          </a-table-column>
          <a-table-column title="最近使用" key="lastUsedAt">
            <template #default="{ record }">
              {{ record.lastUsedAt ? dayjs(record.lastUsedAt).format('YYYY-MM-DD HH:mm') : '-' }}
            </template>
          </a-table-column>
          <a-table-column title="操作" key="actions" width="100">
            <template #default="{ record }">
              <a-button type="link" danger size="small" @click="deletePasskey(record.id)">删除</a-button>
            </template>
          </a-table-column>
        </a-table>
      </div>
    </a-col>
  </a-row>
</template>
