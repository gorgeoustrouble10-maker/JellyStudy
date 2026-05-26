<script setup>
import { ref, watch } from 'vue'
import { authAPI } from '../services/api.js'
import { applyAuthSession } from '../services/userContext.js'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'success'])

const mode = ref('login')
const username = ref('')
const password = ref('')
const displayName = ref('')
const loading = ref(false)
const error = ref('')
const hint = ref(null)

const loadHint = async () => {
  try {
    const res = await authAPI.hint()
    hint.value = res.data
  } catch {
    hint.value = { demo: 'demo / demo123', student: '32308117 / 123456' }
  }
}

watch(() => props.open, (v) => {
  if (v) {
    error.value = ''
    loadHint()
  }
})

const submit = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = mode.value === 'login'
      ? await authAPI.login(username.value, password.value)
      : await authAPI.register(username.value, password.value, displayName.value)
    applyAuthSession(res.data)
    emit('success', res.data)
    emit('close')
  } catch (e) {
    const d = e?.response?.data
    error.value = d?.message || d?.error || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section
    v-if="open"
    class="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4"
    @click.self="emit('close')"
  >
    <section class="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 animate-fadeIn">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-bold text-gray-900">{{ mode === 'login' ? '登录 JellyStudy' : '注册账号' }}</h3>
        <button type="button" class="text-gray-400 hover:text-gray-600" @click="emit('close')">✕</button>
      </div>

      <div class="flex gap-2 mb-4">
        <button
          type="button"
          class="flex-1 py-2 rounded-lg text-sm font-medium"
          :class="mode === 'login' ? 'bg-primary-100 text-primary-800' : 'bg-gray-100 text-gray-600'"
          @click="mode = 'login'"
        >登录</button>
        <button
          type="button"
          class="flex-1 py-2 rounded-lg text-sm font-medium"
          :class="mode === 'register' ? 'bg-primary-100 text-primary-800' : 'bg-gray-100 text-gray-600'"
          @click="mode = 'register'"
        >注册</button>
      </div>

      <form class="space-y-3" @submit.prevent="submit">
        <div>
          <label class="text-xs text-gray-500">用户名</label>
          <input v-model="username" class="input w-full mt-1" placeholder="学号或用户名" required />
        </div>
        <div v-if="mode === 'register'">
          <label class="text-xs text-gray-500">昵称（可选）</label>
          <input v-model="displayName" class="input w-full mt-1" placeholder="显示名称" />
        </div>
        <div>
          <label class="text-xs text-gray-500">密码</label>
          <input v-model="password" type="password" class="input w-full mt-1" placeholder="至少 6 位" required />
        </div>
        <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
        <button type="submit" class="btn-primary w-full" :disabled="loading">
          {{ loading ? '处理中…' : (mode === 'login' ? '登录' : '注册并登录') }}
        </button>
      </form>

      <p v-if="hint" class="text-xs text-gray-400 mt-4 text-center">
        演示：{{ hint.demo }} · 学号：{{ hint.student }}
      </p>
    </section>
  </section>
</template>
