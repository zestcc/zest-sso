<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { settingsApi } from '@/api/admin'
import type { PasswordPolicyInfo, SettingsInfo } from '@/types'

const loading = ref(false)
const saving = ref(false)
const settings = ref<SettingsInfo | null>(null)
const policy = reactive<PasswordPolicyInfo>({
  minLength: 8,
  requireUppercase: true,
  requireLowercase: true,
  requireDigit: true,
  requireSpecial: false,
  passwordHistoryCount: 3,
  maxAgeDays: 0,
})

async function loadSettings() {
  loading.value = true
  try {
    settings.value = await settingsApi.get()
    if (settings.value.passwordPolicy) {
      Object.assign(policy, settings.value.passwordPolicy)
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function savePolicy() {
  saving.value = true
  try {
    await settingsApi.updatePasswordPolicy({ ...policy })
    message.success('密码策略已保存')
    await loadSettings()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <a-spin :spinning="loading">
    <div class="page-card">
      <a-typography-title :level="4">系统设置</a-typography-title>
      <a-alert
        type="info"
        show-icon
        message="Issuer / Token TTL 等来自 application.yml；密码策略可在下方直接修改并即时生效。"
        style="margin: 16px 0"
      />
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="Issuer">{{ settings?.issuer }}</a-descriptions-item>
        <a-descriptions-item label="JWKS Key ID">{{ settings?.keyId }}</a-descriptions-item>
        <a-descriptions-item label="Access Token TTL">{{ settings?.accessTokenTtl }} 秒</a-descriptions-item>
        <a-descriptions-item label="Refresh Token TTL">{{ settings?.refreshTokenTtl }} 秒</a-descriptions-item>
        <a-descriptions-item label="ID Token TTL">{{ settings?.idTokenTtl }} 秒</a-descriptions-item>
        <a-descriptions-item label="登录限流">{{ settings?.loginRateLimit }} 次 / {{ settings?.loginRateWindowSeconds }} 秒</a-descriptions-item>
        <a-descriptions-item label="账号锁定策略">连续失败 {{ settings?.maxLoginAttempts }} 次锁定 {{ settings?.loginLockMinutes }} 分钟</a-descriptions-item>
        <a-descriptions-item label="管理台路径">{{ settings?.adminConsolePath }}</a-descriptions-item>
      </a-descriptions>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <a-typography-title :level="4">密码策略</a-typography-title>
      <a-form layout="vertical" style="max-width: 520px; margin-top: 16px">
        <a-form-item label="最小长度">
          <a-input-number v-model:value="policy.minLength" :min="8" :max="128" style="width: 100%" />
        </a-form-item>
        <a-form-item label="复杂度要求">
          <a-space direction="vertical">
            <a-checkbox v-model:checked="policy.requireUppercase">必须包含大写字母</a-checkbox>
            <a-checkbox v-model:checked="policy.requireLowercase">必须包含小写字母</a-checkbox>
            <a-checkbox v-model:checked="policy.requireDigit">必须包含数字</a-checkbox>
            <a-checkbox v-model:checked="policy.requireSpecial">必须包含特殊字符</a-checkbox>
          </a-space>
        </a-form-item>
        <a-form-item label="历史密码禁止重复次数">
          <a-input-number v-model:value="policy.passwordHistoryCount" :min="0" :max="24" style="width: 100%" />
        </a-form-item>
        <a-form-item label="密码最长有效期（天，0 表示不限制）">
          <a-input-number v-model:value="policy.maxAgeDays" :min="0" :max="365" style="width: 100%" />
        </a-form-item>
        <a-button type="primary" :loading="saving" @click="savePolicy">保存密码策略</a-button>
      </a-form>
    </div>
  </a-spin>
</template>
