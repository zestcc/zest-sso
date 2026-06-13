<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { channelApi, moduleApi, pluginApi } from '@/api/admin'
import type {
  AlertChannelInfo,
  MfaChannelInfo,
  OptionalModuleInfo,
  PluginConfigInfo,
} from '@/types'

const loading = ref(false)
const modules = ref<OptionalModuleInfo[]>([])
const mfaChannels = ref<MfaChannelInfo[]>([])
const alertTypes = ref<AlertChannelInfo[]>([])
const plugins = ref<PluginConfigInfo[]>([])

const pluginModalOpen = ref(false)
const editingPlugin = ref<PluginConfigInfo | null>(null)
const pluginForm = reactive<{ enabled: boolean; config: Record<string, string> }>({
  enabled: false,
  config: {},
})

async function load() {
  loading.value = true
  try {
    const [m, mf, al, pl] = await Promise.all([
      moduleApi.list(),
      channelApi.listMfa(),
      channelApi.listAlerts(),
      pluginApi.list(),
    ])
    modules.value = m
    mfaChannels.value = mf
    alertTypes.value = al
    plugins.value = pl
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openPluginConfig(plugin: PluginConfigInfo) {
  editingPlugin.value = plugin
  pluginForm.enabled = plugin.enabled
  pluginForm.config = { ...plugin.config }
  for (const key of Object.keys(plugin.configHints)) {
    if (!(key in pluginForm.config)) {
      pluginForm.config[key] = ''
    }
  }
  pluginModalOpen.value = true
}

async function savePluginConfig() {
  if (!editingPlugin.value) return
  try {
    const saved = await pluginApi.save(editingPlugin.value.pluginKey, pluginForm.enabled, pluginForm.config)
    message.success('插件配置已保存')
    pluginModalOpen.value = false
    const idx = plugins.value.findIndex((p) => p.pluginKey === saved.pluginKey)
    if (idx >= 0) plugins.value[idx] = saved
    await load()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <a-typography-title :level="4">可插拔模块</a-typography-title>
    <a-alert
      type="info"
      show-icon
      message="YAML/Helm 控制模块总开关；短信等 SDK 插件需 Maven 引入对应 JAR（-Pwith-sms-plugins），再在下方启用并填写凭据。"
      style="margin-bottom: 16px"
    />
    <a-spin :spinning="loading">
      <a-typography-title :level="5">运行时模块</a-typography-title>
      <a-table :data-source="modules" row-key="key" :pagination="false" style="margin-bottom: 24px">
        <a-table-column title="模块" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" width="160" />
        <a-table-column title="分类" data-index="category" key="category" width="100" />
        <a-table-column title="状态" key="enabled" width="90">
          <template #default="{ record }">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '已启用' : '未启用'" />
          </template>
        </a-table-column>
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>

      <a-typography-title :level="5">SDK 插件（按需打包）</a-typography-title>
      <a-table :data-source="plugins" row-key="pluginKey" :pagination="false" style="margin-bottom: 24px">
        <a-table-column title="插件" data-index="pluginName" key="pluginName" />
        <a-table-column title="键" data-index="pluginKey" key="pluginKey" width="140" />
        <a-table-column title="已安装" key="installed" width="90">
          <template #default="{ record }">
            <a-badge :status="record.installed ? 'processing' : 'default'" :text="record.installed ? '是' : '否'" />
          </template>
        </a-table-column>
        <a-table-column title="已启用" key="enabled" width="90">
          <template #default="{ record }">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '是' : '否'" />
          </template>
        </a-table-column>
        <a-table-column title="已配置" key="configured" width="90">
          <template #default="{ record }">
            <a-badge :status="record.configured ? 'success' : 'warning'" :text="record.configured ? '是' : '否'" />
          </template>
        </a-table-column>
        <a-table-column title="操作" key="actions" width="100">
          <template #default="{ record }">
            <a-button type="link" size="small" :disabled="!record.installed" @click="openPluginConfig(record)">
              配置
            </a-button>
          </template>
        </a-table-column>
      </a-table>

      <a-typography-title :level="5">MFA 通道</a-typography-title>
      <a-table :data-source="mfaChannels" row-key="key" :pagination="false" style="margin-bottom: 24px">
        <a-table-column title="通道" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" />
        <a-table-column title="已安装" key="installed" width="90">
          <template #default="{ record }">
            <a-badge :status="record.installed ? 'processing' : 'default'" :text="record.installed ? '是' : '否'" />
          </template>
        </a-table-column>
        <a-table-column title="可用" key="enabled" width="90">
          <template #default="{ record }">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '是' : '否'" />
          </template>
        </a-table-column>
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>

      <a-typography-title :level="5">告警通道类型</a-typography-title>
      <a-table :data-source="alertTypes" row-key="key" :pagination="false">
        <a-table-column title="通道" data-index="name" key="name" />
        <a-table-column title="键" data-index="key" key="key" />
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>
    </a-spin>

    <a-modal
      v-model:open="pluginModalOpen"
      :title="`配置插件：${editingPlugin?.pluginName ?? ''}`"
      ok-text="保存"
      @ok="savePluginConfig"
    >
      <a-form layout="vertical">
        <a-form-item label="启用">
          <a-switch v-model:checked="pluginForm.enabled" />
        </a-form-item>
        <a-form-item
          v-for="(hint, key) in editingPlugin?.configHints ?? {}"
          :key="key"
          :label="hint"
        >
          <a-input
            v-model:value="pluginForm.config[key]"
            :placeholder="hint"
            :type="key.toLowerCase().includes('secret') || key.toLowerCase().includes('password') ? 'password' : 'text'"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
