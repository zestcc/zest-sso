<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { authApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { credentialToJson, formatWebAuthnError, isWebAuthnSupported, parseAuthenticationOptions } from '@/utils/webauthn'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const passkeyLoading = ref(false)
const step = ref<'login' | 'mfa'>('login')
const mfaToken = ref('')
const passkeySupported = isWebAuthnSupported()

const form = reactive({
  username: 'admin',
  password: '',
  mfaCode: '',
})

async function handleLogin() {
  loading.value = true
  try {
    const result = await auth.login(form.username, form.password)
    if (result.mfaRequired && result.mfaToken) {
      step.value = 'mfa'
      mfaToken.value = result.mfaToken
      message.info('请输入身份验证器中的 6 位验证码')
      return
    }
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleMfa() {
  loading.value = true
  try {
    await auth.verifyMfa(mfaToken.value, form.mfaCode)
    message.success('验证成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '验证失败')
  } finally {
    loading.value = false
  }
}

async function handlePasskeyLogin() {
  passkeyLoading.value = true
  try {
    const options = await authApi.webauthnLoginOptions(form.username || undefined)
    if (options.credentialAvailable === false) {
      message.warning('该账号尚未注册 Passkey，请先使用密码登录，在「个人中心」完成注册后再试')
      return
    }
    const credential = await navigator.credentials.get({
      publicKey: parseAuthenticationOptions(options.publicKey),
    })
    if (!credential) throw new Error('未获取到 Passkey')
    await authApi.webauthnLoginFinish(options.sessionToken, credentialToJson(credential))
    await auth.fetchMe()
    message.success('Passkey 登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    message.error(formatWebAuthnError(e))
  } finally {
    passkeyLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand">
        <div class="logo">Z</div>
        <div>
          <h1>ZestSSO Admin</h1>
          <p>统一身份认证管理平台</p>
        </div>
      </div>

      <a-form v-if="step === 'login'" layout="vertical" :model="form" @finish="handleLogin">
        <a-form-item label="用户名" required>
          <a-input v-model:value="form.username" size="large" />
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password v-model:value="form.password" size="large" />
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">
          登录管理台
        </a-button>
        <a-button
          v-if="passkeySupported"
          size="large"
          block
          style="margin-top: 12px"
          :loading="passkeyLoading"
          @click="handlePasskeyLogin"
        >
          使用 Passkey 登录
        </a-button>
      </a-form>

      <a-form v-else layout="vertical" :model="form" @finish="handleMfa">
        <a-alert type="info" message="已启用 MFA，请输入验证器中的 6 位动态码" show-icon style="margin-bottom: 16px" />
        <a-form-item label="验证码" required>
          <a-input v-model:value="form.mfaCode" size="large" maxlength="6" placeholder="000000" />
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" block :loading="loading">验证并登录</a-button>
        <a-button type="link" block style="margin-top: 8px" @click="step = 'login'">返回重新登录</a-button>
      </a-form>

      <div class="login-hint">
        默认账号 admin / admin123。Passkey 需先密码登录后在「个人中心」注册。
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #141b2d 0%, #2d3561 50%, #4c51bf 100%);
  padding: 24px;
}

.login-panel {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25);
}

.login-brand {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 32px;
}

.logo {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.login-brand h1 { margin: 0; font-size: 24px; }
.login-brand p { margin: 4px 0 0; color: #8c8c8c; }
.login-hint { margin-top: 20px; font-size: 12px; color: #8c8c8c; text-align: center; }
</style>
