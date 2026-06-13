import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/admin'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const loading = ref(false)

  async function fetchMe() {
    loading.value = true
    try {
      user.value = await authApi.me()
      return user.value
    } finally {
      loading.value = false
    }
  }

  async function login(username: string, password: string) {
    loading.value = true
    try {
      const result = await authApi.login(username, password)
      if (!result.mfaRequired && result.user) {
        user.value = result.user
      }
      return result
    } finally {
      loading.value = false
    }
  }

  async function verifyMfa(mfaToken: string, code: string) {
    loading.value = true
    try {
      user.value = await authApi.verifyMfa(mfaToken, code)
      return user.value
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    await authApi.logout()
    user.value = null
  }

  return { user, loading, fetchMe, login, verifyMfa, logout }
})
