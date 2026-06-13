<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="forbidden-page">
    <a-result status="403" title="无权限访问" sub-title="当前账号没有访问该功能的权限，请联系 SSO 管理员。">
      <template #extra>
        <a-button type="primary" @click="router.push('/dashboard')">返回概览</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
</style>
