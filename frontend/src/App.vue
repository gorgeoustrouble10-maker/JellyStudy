<script setup>
import { ref, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { BookOpen, HelpCircle, MessageSquare, Brain, Wand2, Menu, X, LogOut } from 'lucide-vue-next'
import { checkAllServices } from './services/healthCheck.js'
import { userId, displayName, isLoggedIn, clearAuth, applyAuthSession } from './services/userContext.js'
import { authAPI } from './services/api.js'
import {
  applyUiPreferences,
  getStoredPreferences,
  setUiPreference,
  watchSystemTheme,
  unwatchSystemTheme
} from './composables/useUiTheme.js'
import ThemePicker from './components/ThemePicker.vue'

// Defer panel chunks until the tab is actually opened.
const KnowledgePointPanel = defineAsyncComponent(() => import('./components/KnowledgePointPanel.vue'))
const QuestionPanel = defineAsyncComponent(() => import('./components/QuestionPanel.vue'))
const AnswerPanel = defineAsyncComponent(() => import('./components/AnswerPanel.vue'))
const EvaluationPanel = defineAsyncComponent(() => import('./components/EvaluationPanel.vue'))
const CoachPanel = defineAsyncComponent(() => import('./components/CoachPanel.vue'))
const LoginModal = defineAsyncComponent(() => import('./components/LoginModal.vue'))

const loginOpen = ref(false)

const restoreSession = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await authAPI.me()
    applyAuthSession(res.data)
  } catch {
    clearAuth()
  }
}

const logout = async () => {
  try {
    await authAPI.logout()
  } catch {
    /* ignore */
  }
  clearAuth()
}

const onLoginSuccess = () => {
  loginOpen.value = false
}

const onAuthRequired = () => {
  loginOpen.value = true
}

const TAB_IDS = ['knowledge', 'question', 'answer', 'evaluation', 'coach']

const tabFromHash = () => {
  const id = window.location.hash.replace(/^#/, '')
  return TAB_IDS.includes(id) ? id : 'knowledge'
}

const activeTab = ref(tabFromHash())
const sidebarOpen = ref(false)
const uiPrefs = ref(getStoredPreferences())
const resolvedTheme = ref('light')
const health = ref({ status: 'checking', label: '检测中…', results: [] })
let healthTimer

const healthDotClass = () => {
  switch (health.value.status) {
    case 'connected': return 'bg-green-500'
    case 'partial': return 'bg-amber-500'
    case 'offline': return 'bg-red-500'
    default: return 'bg-gray-400'
  }
}

const healthTooltip = () => {
  if (!health.value.results?.length) return health.value.label
  return health.value.results
    .map((r) => {
      const base = `${r.displayLabel || r.label}: ${r.up ? '正常' : '不可用'}`
      if (r.service) return `${base} (${r.service})`
      if (!r.components) return base
      const detail = Object.entries(r.components).map(([k, v]) => `${k}=${v}`).join(', ')
      return `${base} [${detail}]`
    })
    .join('\n')
}

const runHealthCheck = async () => {
  health.value = { status: 'checking', label: '检测中…', results: health.value.results }
  health.value = await checkAllServices()
}

const tabs = [
  { id: 'knowledge', name: '知识点', icon: BookOpen },
  { id: 'question', name: '问题', icon: HelpCircle },
  { id: 'answer', name: '回答', icon: MessageSquare },
  { id: 'evaluation', name: '智能评估', icon: Brain },
  { id: 'coach', name: '成长教练', icon: Wand2 }
]

const setActiveTab = (id) => {
  if (!TAB_IDS.includes(id)) return
  activeTab.value = id
  sidebarOpen.value = false
  const nextHash = id === 'knowledge' ? '' : `#${id}`
  const base = `${window.location.pathname}${window.location.search}`
  history.replaceState(null, '', nextHash ? `${base}${nextHash}` : base)
}

const onHashChange = () => {
  activeTab.value = tabFromHash()
}

const syncThemeFromDom = () => {
  resolvedTheme.value = document.documentElement.getAttribute('data-theme') || 'light'
}

const onThemeMode = (mode) => {
  uiPrefs.value = setUiPreference('mode', mode, uiPrefs.value)
  syncThemeFromDom()
}

const onThemeAccent = (accent) => {
  uiPrefs.value = setUiPreference('accent', accent, uiPrefs.value)
}

onMounted(() => {
  const applied = applyUiPreferences(uiPrefs.value)
  uiPrefs.value = { mode: applied.mode, accent: applied.accent }
  resolvedTheme.value = applied.resolved
  watchSystemTheme((a) => {
    resolvedTheme.value = a.resolved
  })
  window.addEventListener('jellystudy:auth-required', onAuthRequired)
  if (!window.location.hash && activeTab.value !== 'knowledge') {
    setActiveTab(activeTab.value)
  }
  window.addEventListener('hashchange', onHashChange)
  runHealthCheck()
  healthTimer = window.setInterval(runHealthCheck, 15000)
  restoreSession().then(() => {
    /* 浏览只读内容无需强制登录；写操作与 Coach 会自行弹出登录框 */
  })
})

onUnmounted(() => {
  unwatchSystemTheme()
  window.removeEventListener('hashchange', onHashChange)
  window.removeEventListener('jellystudy:auth-required', onAuthRequired)
  if (healthTimer) clearInterval(healthTimer)
})

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

</script>

<style scoped>
.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.tab-fade-enter-from,
.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

<template>
  <!-- eslint-disable vue/no-multiple-template-root -->
  <section class="min-h-screen flex bg-[rgb(var(--surface))]">
    <aside
      class="shell-sidebar fixed lg:sticky top-0 left-0 h-screen w-64 border-r z-50 transform transition-transform duration-300 ease-out lg:translate-x-0"
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <section class="p-6">
        <section class="flex items-center gap-3 mb-8">
          <section class="brand-icon">
            <BookOpen class="w-6 h-6 text-white" />
          </section>
          <section>
            <h1 class="text-xl font-bold text-[rgb(var(--text))]">JellyStudy</h1>
            <p class="text-xs text-[rgb(var(--text-muted))]">学习问答系统</p>
          </section>
        </section>

        <nav class="space-y-2">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="setActiveTab(tab.id)"
            class="nav-tab"
            :class="activeTab === tab.id ? 'nav-tab-active' : 'nav-tab-idle'"
          >
            <component :is="tab.icon" class="w-5 h-5" />
            <span>{{ tab.name }}</span>
          </button>
        </nav>

        <ThemePicker
          :mode="uiPrefs.mode"
          :accent="uiPrefs.accent"
          :resolved="resolvedTheme"
          @update:mode="onThemeMode"
          @update:accent="onThemeAccent"
        />

        <section class="health-card mt-4">
          <p class="text-sm text-[rgb(var(--text-muted))] mb-1">服务状态</p>
          <section class="flex items-center gap-2" :title="healthTooltip()">
            <span
              class="w-2 h-2 rounded-full"
              :class="[healthDotClass(), health.status === 'connected' ? 'animate-pulse' : '']"
            />
            <span class="text-sm font-medium text-[rgb(var(--text))]">{{ health.label }}</span>
          </section>
          <ul v-if="health.results.length" class="mt-2 space-y-1 text-xs text-[rgb(var(--text-muted))]">
            <li v-for="r in health.results" :key="r.key" class="flex items-center gap-1.5">
              <span class="w-1.5 h-1.5 rounded-full" :class="r.up ? 'bg-green-500' : 'bg-red-400'" />
              {{ r.displayLabel || r.label }}
            </li>
          </ul>
        </section>
      </section>
    </aside>

    <section
      v-if="sidebarOpen"
      @click="sidebarOpen = false"
      class="fixed inset-0 bg-black/50 z-40 lg:hidden"
    />

    <main class="flex-1 lg:ml-64 main-canvas">
      <header class="shell-header">
        <section class="flex items-center justify-between px-6 py-4">
          <section class="flex items-center gap-4">
            <button
              @click="toggleSidebar"
              class="lg:hidden p-2 rounded-lg transition-all duration-200 hover:bg-[rgb(var(--surface-muted))] active:scale-95"
            >
              <Menu v-if="!sidebarOpen" class="w-6 h-6 text-muted" />
              <X v-else class="w-6 h-6 text-muted" />
            </button>
            <section>
              <h2 class="text-lg font-semibold text-[rgb(var(--text))]">
                {{ tabs.find(t => t.id === activeTab)?.name }}{{ activeTab === 'evaluation' ? '服务' : activeTab === 'coach' ? '' : '管理' }}
              </h2>
              <p class="text-sm text-[rgb(var(--text-muted))]">
                {{ activeTab === 'evaluation' ? '基于大模型的智能评估' : activeTab === 'coach' ? 'AI 学情诊断 · 巩固出题 · 知识宠物' : '高效管理您的学习资源' }}
              </p>
            </section>
          </section>
          <section class="flex items-center gap-3">
            <template v-if="isLoggedIn">
              <section class="hidden sm:flex flex-col items-end">
                <span class="text-sm font-medium text-[rgb(var(--text))]">{{ displayName || userId }}</span>
                <span class="text-xs text-[rgb(var(--text-muted))]">@{{ userId }}</span>
              </section>
              <button
                type="button"
                class="btn-secondary text-xs py-1.5 flex items-center gap-1"
                title="退出登录"
                @click="logout"
              >
                <LogOut class="w-3.5 h-3.5" />
                退出
              </button>
            </template>
            <button
              v-else
              type="button"
              class="btn-primary text-sm py-1.5"
              @click="loginOpen = true"
            >
              登录 / 注册
            </button>
            <section
              class="hidden sm:flex items-center gap-2 px-4 py-2 rounded-full bg-[rgb(var(--surface-muted))]"
              :title="healthTooltip()"
            >
              <span class="w-2 h-2 rounded-full" :class="healthDotClass()" />
              <span class="text-sm text-[rgb(var(--text-muted))]">{{ health.label }}</span>
            </section>
            <button
              type="button"
              class="w-10 h-10 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-semibold text-sm transition-all duration-200 hover:opacity-90 hover:scale-105 active:scale-95"
              :title="isLoggedIn ? `当前用户: ${userId}` : '点击登录'"
              @click="isLoggedIn ? undefined : (loginOpen = true)"
            >
              {{ (displayName || userId || 'J').charAt(0).toUpperCase() }}
            </button>
          </section>
        </section>
      </header>

      <section class="p-6 relative z-[1]">
        <Transition name="tab-fade" mode="out-in">
          <div :key="activeTab" class="animate-fadeSlide">
            <KnowledgePointPanel v-if="activeTab === 'knowledge'" />
            <QuestionPanel v-else-if="activeTab === 'question'" />
            <AnswerPanel v-else-if="activeTab === 'answer'" />
            <EvaluationPanel v-else-if="activeTab === 'evaluation'" />
            <CoachPanel v-else-if="activeTab === 'coach'" />
          </div>
        </Transition>
      </section>
    </main>

    <LoginModal :open="loginOpen" @close="loginOpen = false" @success="onLoginSuccess" />
  </section>
</template>
