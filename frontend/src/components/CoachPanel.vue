<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { coachAPI } from '../services/api.js'
import { userId, isLoggedIn } from '../services/userContext.js'

const loading = ref(false)
const actionLoading = ref('')
const profile = ref(null)
const tasks = ref([])
const pet = ref(null)
const report = ref(null)
const quiz = ref(null)
const quizAnswer = ref('')
const quizResult = ref(null)
const nacosConfig = ref(null)
const leaderboard = ref([])
const error = ref('')
const success = ref('')
const quizSection = ref(null)

const socraticTopic = ref('')
const socraticMessage = ref('')
/** 当前话题下的连续对话 [{ role, content, hint? }] */
const socraticMessages = ref([])
const socraticChatBox = ref(null)
const socraticTurnCount = ref(0)
const socraticSummary = ref(null)
const lastAssistantHints = ref({ levels: [], revealed: 0 })
const showCompareMode = ref(false)
const trendChartEl = ref(null)
let trendChartInstance = null

const DEFAULT_THEME = 'default'

const themeEmoji = (theme) => {
  if (!theme || theme === DEFAULT_THEME) return null
  if (theme.includes('Redis')) return '🔥'
  if (theme.includes('数据库')) return '🗄️'
  if (theme.includes('操作系统')) return '⚙️'
  if (theme.includes('计算机组成')) return '💻'
  if (theme.includes('1+1')) return '🔢'
  return '✨'
}

const appearanceEmoji = (appearance) => ({
  egg: '🥚',
  chick: '🐥',
  juvenile: '🐤',
  teen: '🦎',
  dragon: '🐉'
}[appearance] || '🐣')

const originalPetEmoji = (p) => {
  if (!p) return '🥚'
  if (p.baseAppearance) return appearanceEmoji(p.baseAppearance)
  if (p.appearance && p.appearance !== 'theme') return appearanceEmoji(p.appearance)
  if (p.level >= 5) return '🐉'
  if (p.level >= 4) return '🦎'
  if (p.level >= 3) return '🐤'
  if (p.level >= 2) return '🐥'
  return '🐣'
}

const basePetEmoji = computed(() => originalPetEmoji(pet.value))

const evolutionLabel = computed(() => {
  if (!pet.value) return ''
  if (pet.value.baseAppearanceLabel) return pet.value.baseAppearanceLabel
  if (pet.value.currentTheme) return pet.value.baseAppearanceLabel || `Lv.${pet.value.level} 等级形象`
  const labels = { egg: 'Lv.1 初生', chick: 'Lv.2 幼鸟', juvenile: 'Lv.3 少年', teen: 'Lv.4 青年', dragon: 'Lv.5 完全体' }
  return labels[pet.value.baseAppearance || pet.value.appearance] || `Lv.${pet.value.level}`
})

const checkInDots = computed(() => profile.value?.recent7DayCheckIns || [])
const checkInDots30 = computed(() => profile.value?.recent30DayCheckIns || [])
const checkInDayLabels = ['-6', '-5', '-4', '-3', '-2', '昨天', '今天']

const themePetEmoji = computed(() => {
  if (!pet.value?.currentTheme) return basePetEmoji.value
  return themeEmoji(pet.value.currentTheme) || '✨'
})

const themeLabel = computed(() => pet.value?.currentTheme || '原始形态')

const hasThemeSkin = computed(() => !!pet.value?.currentTheme)

const petEmoji = computed(() => {
  if (showCompareMode.value && hasThemeSkin.value) return themePetEmoji.value
  if (!pet.value) return '🥚'
  if (pet.value.currentTheme) return themePetEmoji.value
  return basePetEmoji.value
})

const activeThemeId = computed(() => pet.value?.currentTheme || DEFAULT_THEME)

const themeOptions = computed(() => {
  const opts = [{ id: DEFAULT_THEME, label: '原始形态', emoji: basePetEmoji.value }]
  for (const t of pet.value?.unlockedThemes || []) {
    opts.push({ id: t, label: t, emoji: themeEmoji(t) || '✨' })
  }
  return opts
})

const learnedPoints = computed(() => profile.value?.learnedKnowledgePoints || [])

const pointsProgress = computed(() => {
  const r = report.value
  if (!r?.weeklyGoalPoints) return 0
  return Math.min(100, Math.round((r.totalPoints / r.weeklyGoalPoints) * 100))
})

const streakWeekDots = computed(() => {
  const days = report.value?.recent7DayCheckIns || profile.value?.recent7DayCheckIns || []
  const labels = ['-6', '-5', '-4', '-3', '-2', '昨天', '今天']
  return labels.map((day, i) => ({ day, active: !!days[i] }))
})

const knowledgeBars = computed(() => {
  const mastery = report.value?.knowledgeMastery || profile.value?.knowledgeMastery
  if (mastery?.length) {
    return mastery.map((m) => ({
      name: m.name,
      percent: m.percent,
      status: m.status,
      source: m.source
    }))
  }
  const r = report.value
  if (!r?.learnedKnowledgePoints?.length) return []
  const weak = r.weakPoints || []
  const isWeak = (name) => weak.some((w) => w === name || w.includes(name) || name.includes(w))
  return r.learnedKnowledgePoints.map((name) => ({
    name,
    percent: isWeak(name) ? 55 : 0,
    status: isWeak(name) ? '待巩固' : '未练习',
    source: 'diagnosis'
  }))
})

const escapeHtml = (s) => String(s)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')

const summaryHtml = computed(() => {
  const text = report.value?.aiSummary
  if (!text) return ''
  return text.split('\n').map((line) => {
    const t = line.trim()
    if (!t) return ''
    if (t.startsWith('## ')) return `<h4 class="font-semibold text-gray-800 mt-3 mb-1">${escapeHtml(t.slice(3))}</h4>`
    if (t.startsWith('# ')) return `<h3 class="font-bold text-lg text-gray-900 mt-2 mb-1">${escapeHtml(t.slice(2))}</h3>`
    if (t.startsWith('- ')) return `<li class="ml-4 text-gray-700 list-disc">${escapeHtml(t.slice(2))}</li>`
    return `<p class="text-gray-700 leading-relaxed my-1">${escapeHtml(t)}</p>`
  }).join('')
})

const extractError = (e) => {
  const d = e?.response?.data
  return d?.message || d?.error || e?.message || '操作失败，请确认 Coach 服务 (8084) 与知识点服务 (8081) 已启动'
}

const flashSuccess = (msg) => {
  success.value = msg
  error.value = ''
  setTimeout(() => { success.value = '' }, 4000)
}

const loadLeaderboard = async () => {
  try {
    const res = await coachAPI.getLeaderboard(10)
    leaderboard.value = Array.isArray(res.data) ? res.data : []
  } catch {
    leaderboard.value = []
  }
}

const loadAll = async () => {
  loading.value = true
  error.value = ''
  try {
    const [p, t, petRes, cfg] = await Promise.all([
      coachAPI.getProfile(),
      coachAPI.getTodayTasks(),
      coachAPI.getPet(),
      coachAPI.getConfig()
    ])
    profile.value = p.data
    tasks.value = Array.isArray(t.data) ? t.data : []
    pet.value = petRes.data
    nacosConfig.value = cfg.data
    if (!socraticTopic.value && profile.value?.learnedKnowledgePoints?.length) {
      socraticTopic.value = profile.value.learnedKnowledgePoints[0]
    }
    await loadLeaderboard()
  } catch (e) {
    error.value = extractError(e)
  } finally {
    loading.value = false
  }
}

const syncKnowledge = async () => {
  actionLoading.value = 'sync'
  error.value = ''
  try {
    const res = await coachAPI.syncKnowledge()
    profile.value = res.data
    const t = await coachAPI.getTodayTasks()
    tasks.value = Array.isArray(t.data) ? t.data : []
    flashSuccess(`已同步 ${profile.value.learnedKnowledgePoints?.length || 0} 个知识点`)
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const switchTheme = async (themeId) => {
  if (themeId === activeThemeId.value) return
  actionLoading.value = 'theme'
  error.value = ''
  try {
    const res = await coachAPI.switchPetTheme(themeId)
    pet.value = res.data
    showCompareMode.value = themeId !== DEFAULT_THEME && !!pet.value?.currentTheme
    const label = themeId === DEFAULT_THEME ? '原始形态' : themeId
    flashSuccess(`已切换为：${label}`)
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const feedPet = async () => {
  actionLoading.value = 'feed'
  error.value = ''
  try {
    const res = await coachAPI.feedPet(10)
    pet.value = res.data
    flashSuccess('喂养成功！小果冻很开心～')
    await loadAll()
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const loadReport = async () => {
  actionLoading.value = 'report'
  error.value = ''
  report.value = null
  try {
    const res = await coachAPI.getWeeklyReport()
    report.value = res.data
    flashSuccess('AI 周报已生成')
    await nextTick(renderTrendChart)
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const startQuiz = async (weakPoint) => {
  actionLoading.value = 'quiz'
  error.value = ''
  quizResult.value = null
  quizAnswer.value = ''
  quiz.value = null
  try {
    const res = await coachAPI.generateQuiz(weakPoint)
    const list = Array.isArray(res.data) ? res.data : []
    if (!list.length) {
      error.value = 'AI 未能生成题目，请稍后重试'
      return
    }
    quiz.value = list[0]
    flashSuccess(`已生成「${weakPoint}」巩固题，请向下作答`)
    await nextTick()
    quizSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const submitQuiz = async () => {
  if (!quiz.value || !quizAnswer.value.trim()) return
  actionLoading.value = 'submit'
  error.value = ''
  try {
    const res = await coachAPI.submitQuiz(quiz.value.id, quizAnswer.value.trim())
    quizResult.value = res.data
    flashSuccess(`批改完成！得分 ${res.data.score}。${res.data.checkInMessage || '已获得积分奖励'}`)
    await loadAll()
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const scrollSocraticToBottom = async () => {
  await nextTick()
  socraticChatBox.value?.scrollTo({ top: socraticChatBox.value.scrollHeight, behavior: 'smooth' })
}

const clearSocraticChat = () => {
  socraticMessages.value = []
  socraticTurnCount.value = 0
  socraticMessage.value = ''
  socraticSummary.value = null
  lastAssistantHints.value = { levels: [], revealed: 0 }
}

watch(socraticTopic, (next, prev) => {
  if (prev && next !== prev && socraticMessages.value.length) {
    clearSocraticChat()
  }
})

const askSocratic = async () => {
  if (!socraticTopic.value || !socraticMessage.value.trim()) return
  const userText = socraticMessage.value.trim()
  actionLoading.value = 'socratic'
  error.value = ''
  socraticMessages.value.push({ role: 'user', content: userText })
  socraticMessage.value = ''
  await scrollSocraticToBottom()
  try {
    const history = socraticMessages.value.slice(0, -1)
    const res = await coachAPI.socraticAsk(socraticTopic.value, userText, history)
    const levels = [res.data.hintLevel1, res.data.hintLevel2, res.data.hintLevel3].filter(Boolean)
    socraticMessages.value.push({
      role: 'assistant',
      content: res.data.reply,
      hint: res.data.hint,
      hintLevels: levels,
      revealedHint: 0,
      misconception: res.data.misconceptionDetected
    })
    lastAssistantHints.value = { levels, revealed: 0 }
    socraticTurnCount.value = res.data.turnCount || Math.ceil(socraticMessages.value.length / 2)
    await scrollSocraticToBottom()
  } catch (e) {
    socraticMessages.value.pop()
    socraticMessage.value = userText
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const revealHint = (level) => {
  const last = [...socraticMessages.value].reverse().find((m) => m.role === 'assistant')
  if (last && last.hintLevels?.length) {
    last.revealedHint = Math.max(last.revealedHint || 0, level)
  }
}

const finishSocratic = async () => {
  if (socraticMessages.value.length < 2) {
    error.value = '请至少完成一轮问答后再生成总结'
    return
  }
  actionLoading.value = 'summary'
  error.value = ''
  try {
    const res = await coachAPI.socraticSummary(socraticTopic.value, socraticMessages.value)
    socraticSummary.value = res.data
    flashSuccess('已生成知识点总结并更新学情评估')
    await loadAll()
  } catch (e) {
    error.value = extractError(e)
  } finally {
    actionLoading.value = ''
  }
}

const isBusy = (key) => actionLoading.value === key

const renderTrendChart = async () => {
  const trend = report.value?.pointsTrend
  if (!trendChartEl.value || !trend?.labels?.length) return
  if (!trendChartInstance) trendChartInstance = echarts.init(trendChartEl.value)
  trendChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: trend.labels, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '积分' },
    series: [{ name: '总积分', type: 'line', data: trend.points, smooth: true, areaStyle: { opacity: 0.15 } }]
  })
}

watch(report, () => nextTick(renderTrendChart))
watch(userId, () => {
  report.value = null
  socraticSummary.value = null
  clearSocraticChat()
  showCompareMode.value = false
  if (isLoggedIn.value) loadAll()
})

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) loadAll()
})

onMounted(() => {
  if (isLoggedIn.value) loadAll()
})
</script>

<template>
  <section class="space-y-6 animate-fadeIn">
    <section v-if="loading && !profile" class="card p-8 text-center text-gray-500">
      <p class="text-lg">正在加载成长教练数据…</p>
    </section>

    <section v-if="error" class="p-4 bg-red-50 text-red-700 rounded-xl border border-red-100">
      {{ error }}
    </section>
    <section v-if="success" class="p-4 bg-green-50 text-green-700 rounded-xl border border-green-100">
      {{ success }}
    </section>

    <section v-if="actionLoading" class="p-3 bg-primary-50 text-primary-700 rounded-xl text-sm flex items-center gap-2">
      <span class="inline-block w-4 h-4 border-2 border-primary-600 border-t-transparent rounded-full animate-spin" />
      <span v-if="actionLoading === 'quiz'">AI 正在出题，约需 3～10 秒…</span>
      <span v-else-if="actionLoading === 'submit'">AI 正在批改答案…</span>
      <span v-else-if="actionLoading === 'report'">AI 正在撰写周报…</span>
      <span v-else-if="actionLoading === 'feed'">正在喂养宠物…</span>
      <span v-else-if="actionLoading === 'socratic'">苏格拉底助教思考中…</span>
      <span v-else-if="actionLoading === 'summary'">正在生成总结并联动学情评估…</span>
      <span v-else-if="actionLoading === 'sync'">正在从知识点库同步…</span>
      <span v-else-if="actionLoading === 'theme'">正在切换宠物形态…</span>
      <span v-else>处理中…</span>
    </section>

    <section v-if="!isLoggedIn" class="card p-8 text-center">
      <p class="text-lg text-gray-700 mb-2">请先登录后再使用成长教练</p>
      <p class="text-sm text-gray-500">顶栏点击「登录 / 注册」，数据将按账号隔离存储</p>
    </section>

    <template v-else-if="profile">
      <section class="grid lg:grid-cols-3 gap-6">
        <section class="card p-6 text-center">
          <p class="text-sm text-gray-500 mb-2">知识宠物 · JellyCoach</p>

          <!-- 并排对比：主题皮肤 vs 等级原始形象 -->
          <div v-if="showCompareMode && hasThemeSkin" class="flex items-stretch justify-center gap-3 my-4">
            <div class="flex-1 max-w-[140px] rounded-xl border-2 border-primary-200 bg-primary-50/50 p-3">
              <p class="text-[10px] text-primary-600 font-medium mb-1">主题皮肤</p>
              <div class="text-5xl">{{ themePetEmoji }}</div>
              <p class="text-xs text-gray-600 mt-2 truncate" :title="themeLabel">{{ themeLabel }}</p>
            </div>
            <div class="flex items-center text-gray-300 text-sm font-light">VS</div>
            <div class="flex-1 max-w-[140px] rounded-xl border-2 border-green-200 bg-green-50/50 p-3">
              <p class="text-[10px] text-green-600 font-medium mb-1">等级原始</p>
              <div class="text-5xl">{{ basePetEmoji }}</div>
              <p class="text-xs text-gray-600 mt-2">{{ evolutionLabel }}</p>
            </div>
          </div>

          <!-- 单形象展示 -->
          <div v-else class="relative inline-block">
            <div class="text-7xl my-4 transition-all">{{ petEmoji }}</div>
          </div>

          <button
            v-if="hasThemeSkin"
            type="button"
            class="text-xs text-primary-600 hover:underline mb-2"
            @click="showCompareMode = !showCompareMode"
          >
            {{ showCompareMode ? '收起对比' : '并排对比：主题 vs 等级原始' }}
          </button>

          <h3 class="text-xl font-bold">{{ pet?.petName || '小果冻' }}</h3>
          <p class="text-gray-500 text-sm">Lv.{{ pet?.level || 1 }} · {{ pet?.mood || 'happy' }}</p>
          <p v-if="!pet?.currentTheme && evolutionLabel" class="text-xs text-green-600 mt-1">{{ evolutionLabel }}</p>
          <p class="text-xs text-purple-600 mt-1">
            当前形态：{{ pet?.currentTheme || '原始形态（随等级进化）' }}
          </p>
          <div class="mt-3 text-left">
            <p class="text-xs text-gray-500 mb-2 text-center">切换形态（原始 + 已解锁主题）</p>
            <div class="flex flex-wrap gap-2 justify-center">
              <button
                v-for="opt in themeOptions"
                :key="opt.id"
                type="button"
                class="px-2 py-1 rounded-full text-xs border transition-colors"
                :class="activeThemeId === opt.id
                  ? 'bg-primary-100 border-primary-400 text-primary-800'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
                :disabled="!!actionLoading"
                @click="switchTheme(opt.id)"
              >
                {{ opt.emoji }} {{ opt.label }}
              </button>
            </div>
          </div>
          <div class="mt-4 bg-gray-100 rounded-full h-3 overflow-hidden">
            <div
              class="h-full bg-primary-500 transition-all"
              :style="{ width: `${((pet?.experience || 0) / (pet?.experienceToNext || 50)) * 100}%` }"
            />
          </div>
          <p class="text-xs text-gray-400 mt-1">{{ pet?.experience || 0 }} / {{ pet?.experienceToNext || 50 }} EXP</p>
          <p v-if="pet?.unlockedThemes?.length" class="text-xs text-gray-500 mt-2">
            已解锁主题：{{ pet.unlockedThemes.join('、') }}（可随时切换，不影响原始形态）
          </p>
          <p class="text-xs text-amber-600 mt-2">当前积分 {{ profile?.totalPoints ?? 0 }}，喂养需 10 分</p>
          <button
            class="btn-primary mt-4 w-full"
            :disabled="!!actionLoading || (profile?.totalPoints ?? 0) < 10"
            @click="feedPet"
          >
            {{ isBusy('feed') ? '喂养中…' : '喂养 (-10 积分)' }}
          </button>
        </section>

        <section class="card p-6 lg:col-span-2">
          <div class="flex flex-wrap items-center justify-between gap-2 mb-3">
            <h3 class="font-semibold text-lg">成长档案</h3>
            <button class="btn-secondary text-sm" :disabled="!!actionLoading" @click="syncKnowledge">
              {{ isBusy('sync') ? '同步中…' : '同步知识点库' }}
            </button>
          </div>
          <div class="grid sm:grid-cols-3 gap-4 mb-4">
            <div class="bg-primary-50 rounded-xl p-4 text-center">
              <p class="text-2xl font-bold text-primary-700">{{ profile?.totalPoints ?? 0 }}</p>
              <p class="text-xs text-gray-500">总积分</p>
            </div>
            <div class="bg-amber-50 rounded-xl p-4 text-center">
              <p class="text-2xl font-bold text-amber-700">{{ profile?.streakDays ?? 0 }}</p>
              <p class="text-xs text-gray-500">连续打卡（自然日）</p>
            </div>
            <div class="bg-green-50 rounded-xl p-4 text-center">
              <p class="text-2xl font-bold text-green-700">{{ learnedPoints.length }}</p>
              <p class="text-xs text-gray-500">已学知识点</p>
            </div>
          </div>
          <div v-if="checkInDots30.length" class="mb-4 p-3 bg-amber-50/50 rounded-lg">
            <p class="text-xs text-amber-800 mb-2">{{ profile?.streakNote }} · 30 天日历</p>
            <div class="flex flex-wrap gap-1 justify-center max-w-md mx-auto">
              <div
                v-for="(on, i) in checkInDots30"
                :key="'d30-' + i"
                class="w-3 h-3 rounded-sm"
                :class="on ? 'bg-amber-500' : 'bg-gray-200'"
                :title="'第' + (i + 1) + '天'"
              />
            </div>
          </div>
          <div v-if="checkInDots.length" class="mb-4 p-3 bg-amber-50/30 rounded-lg">
            <p class="text-xs text-amber-700 mb-2">近 7 日</p>
            <div class="flex gap-1 justify-center">
              <div v-for="(on, i) in checkInDots" :key="i" class="flex flex-col items-center">
                <div class="w-6 h-6 rounded-full text-[10px] flex items-center justify-center"
                  :class="on ? 'bg-amber-500 text-white' : 'bg-gray-200 text-gray-400'">{{ on ? '✓' : '' }}</div>
                <span class="text-[9px] text-gray-500 mt-0.5">{{ checkInDayLabels[i] }}</span>
              </div>
            </div>
          </div>
          <p class="text-sm text-gray-600 bg-gray-50 rounded-lg p-3">{{ profile?.lastDiagnosis }}</p>
          <p v-if="profile?.quizScopeSource" class="text-xs text-gray-400 mt-2">{{ profile.quizScopeSource }}</p>
          <div v-if="learnedPoints.length" class="mt-3">
            <span class="text-sm font-medium">已学知识点（AI 出题白名单）：</span>
            <span
              v-for="kp in learnedPoints"
              :key="kp"
              class="inline-block mr-2 mt-1 px-2 py-0.5 bg-blue-50 text-blue-700 rounded-full text-xs"
            >{{ kp }}</span>
          </div>
          <p v-if="profile?.weakPoints?.length" class="mt-3 text-sm">
            <span class="font-medium">薄弱点（点击练习）：</span>
            <button
              v-for="wp in profile.weakPoints"
              :key="wp"
              type="button"
              class="inline-block mr-2 mt-1 px-2 py-0.5 bg-red-50 text-red-700 rounded-full text-xs hover:bg-red-100"
              :disabled="!!actionLoading"
              @click="startQuiz(wp)"
            >{{ wp }} → 练习</button>
          </p>
        </section>
      </section>

      <section class="grid lg:grid-cols-2 gap-6">
        <section class="card p-6">
          <h3 class="font-semibold text-lg mb-3">今日 AI 推荐任务</h3>
          <p v-if="!tasks.length" class="text-sm text-gray-500">暂无任务，点击「同步知识点库」后刷新</p>
          <ul v-else class="space-y-2">
            <li
              v-for="task in tasks"
              :key="task.taskId"
              class="flex items-center justify-between p-3 rounded-lg"
              :class="task.completed ? 'bg-green-50 border border-green-100' : 'bg-gray-50'"
            >
              <span :class="task.completed ? 'text-green-800 line-through decoration-green-400' : ''">{{ task.title }}</span>
              <span v-if="task.completed" class="text-xs text-green-600 font-medium">已完成 ✓</span>
              <button
                v-else
                type="button"
                class="text-primary-600 text-sm hover:underline disabled:opacity-50"
                :disabled="!!actionLoading"
                @click="startQuiz(task.weakPoint)"
              >
                {{ isBusy('quiz') ? '出题中…' : `开始 (+${task.rewardPoints}分)` }}
              </button>
            </li>
          </ul>
        </section>

        <section class="card p-6">
          <h3 class="font-semibold text-lg mb-3">积分排行榜 (Redis ZSET)</h3>
          <p v-if="!leaderboard.length" class="text-sm text-gray-500">暂无排行，完成练习后上榜</p>
          <ol v-else class="space-y-2">
            <li
              v-for="entry in leaderboard"
              :key="entry.userId"
              class="flex items-center justify-between p-2 rounded-lg"
              :class="entry.userId === userId ? 'bg-primary-50' : 'bg-gray-50'"
            >
              <span>#{{ entry.rank }} {{ entry.userId }}</span>
              <span class="font-semibold text-primary-700">{{ entry.points }} 分</span>
            </li>
          </ol>
        </section>
      </section>

      <section ref="quizSection" v-if="quiz" class="card p-6 border-2 border-primary-100">
        <h3 class="font-semibold text-lg mb-2">AI 巩固练习</h3>
        <p class="text-gray-800 mb-2">{{ quiz.question }}</p>
        <p class="text-xs text-gray-400 mb-3">提示：{{ quiz.hint }}</p>
        <textarea v-model="quizAnswer" class="input w-full mb-3" rows="3" placeholder="写下你的答案…" />
        <button
          class="btn-primary"
          :disabled="!!actionLoading || !quizAnswer.trim()"
          @click="submitQuiz"
        >
          {{ isBusy('submit') ? 'AI 批改中…' : '提交 AI 批改' }}
        </button>
        <section v-if="quizResult" class="mt-4 p-4 rounded-lg text-sm" :class="quizResult.score >= 80 ? 'bg-green-50' : quizResult.score >= 60 ? 'bg-amber-50' : 'bg-red-50'">
          <p>
            <strong>得分：</strong>
            <span class="text-lg font-bold" :class="quizResult.score >= 80 ? 'text-green-700' : quizResult.score >= 60 ? 'text-amber-700' : 'text-red-700'">
              {{ quizResult.score }}
            </span>
            <span class="text-xs text-gray-500 ml-2">（千问真实批改 · 按答案质量梯度打分）</span>
          </p>
          <p class="mt-1">{{ quizResult.feedback }}</p>
          <p v-if="quizResult.score >= 80" class="text-purple-600 mt-1">高分！已解锁对应宠物主题</p>
        </section>
      </section>

      <section class="card p-6 flex flex-col">
        <div class="flex flex-wrap items-center justify-between gap-2 mb-2">
          <div>
            <h3 class="font-semibold text-lg">苏格拉底式追问</h3>
            <p class="text-sm text-gray-500">多轮对话 · 三级提示 · 错误反向纠正 · 结束可生成总结</p>
          </div>
          <div class="flex items-center gap-2 flex-wrap">
            <span v-if="socraticTurnCount" class="text-xs text-primary-600 bg-primary-50 px-2 py-1 rounded-full">
              第 {{ socraticTurnCount }} 轮
            </span>
            <button
              v-if="socraticMessages.length >= 2"
              type="button"
              class="btn-primary text-xs"
              :disabled="!!actionLoading"
              @click="finishSocratic"
            >
              {{ isBusy('summary') ? '生成中…' : '结束并生成总结' }}
            </button>
            <button
              v-if="socraticMessages.length"
              type="button"
              class="btn-secondary text-xs"
              :disabled="!!actionLoading"
              @click="clearSocraticChat"
            >
              清空对话
            </button>
          </div>
        </div>

        <div class="flex items-center gap-2 mb-3">
          <label class="text-xs text-gray-500 shrink-0">话题</label>
          <select v-model="socraticTopic" class="input flex-1 max-w-xs">
            <option v-for="kp in learnedPoints" :key="kp" :value="kp">{{ kp }}</option>
          </select>
        </div>

        <div
          ref="socraticChatBox"
          class="flex-1 min-h-[220px] max-h-80 overflow-y-auto rounded-xl border border-gray-100 bg-gray-50/80 p-4 space-y-3 mb-3"
        >
          <p v-if="!socraticMessages.length" class="text-sm text-gray-400 text-center py-8">
            像聊天一样连续追问。例如先问「什么是阻塞态？」，再根据助教引导继续答下去。
          </p>
          <template v-for="(msg, idx) in socraticMessages" :key="idx">
            <div v-if="msg.role === 'user'" class="flex justify-end">
              <div class="max-w-[85%] bg-primary-600 text-white rounded-2xl rounded-tr-sm px-4 py-2 text-sm">
                {{ msg.content }}
              </div>
            </div>
            <div v-else class="flex justify-start">
              <div class="max-w-[85%] bg-white border border-gray-200 rounded-2xl rounded-tl-sm px-4 py-2 text-sm shadow-sm">
                <p v-if="msg.misconception" class="text-[10px] text-amber-600 mb-1">↩ 反向引导（帮你发现矛盾）</p>
                <p class="text-primary-800">{{ msg.content }}</p>
                <p v-if="msg.hint" class="text-xs text-gray-400 mt-1 border-t border-gray-100 pt-1">💡 {{ msg.hint }}</p>
                <div v-if="msg.hintLevels?.length && idx === socraticMessages.length - 1" class="mt-2 flex flex-wrap gap-1">
                  <button
                    v-for="lv in 3"
                    :key="lv"
                    type="button"
                    class="text-[10px] px-2 py-0.5 rounded border"
                    :class="msg.revealedHint >= lv ? 'bg-amber-100 border-amber-300 text-amber-800' : 'bg-gray-50 border-gray-200 text-gray-500'"
                    :disabled="!msg.hintLevels[lv - 1]"
                    @click="revealHint(lv)"
                  >{{ lv }}级提示</button>
                </div>
                <p v-if="msg.revealedHint >= 1 && msg.hintLevels[0]" class="text-xs text-amber-700 mt-1">
                  L1：{{ msg.hintLevels[0] }}
                </p>
                <p v-if="msg.revealedHint >= 2 && msg.hintLevels[1]" class="text-xs text-amber-700">
                  L2：{{ msg.hintLevels[1] }}
                </p>
                <p v-if="msg.revealedHint >= 3 && msg.hintLevels[2]" class="text-xs text-amber-800 font-medium">
                  L3：{{ msg.hintLevels[2] }}
                </p>
              </div>
            </div>
          </template>
          <div v-if="isBusy('socratic')" class="flex justify-start">
            <div class="bg-white border border-gray-200 rounded-2xl px-4 py-2 text-sm text-gray-500 flex items-center gap-2">
              <span class="inline-block w-3 h-3 border-2 border-primary-400 border-t-transparent rounded-full animate-spin" />
              助教思考中…
            </div>
          </div>
        </div>

        <div class="flex gap-2">
          <input
            v-model="socraticMessage"
            class="input flex-1"
            placeholder="继续追问或回答助教的问题…"
            @keyup.enter="askSocratic"
          />
          <button class="btn-primary shrink-0" :disabled="!!actionLoading || !socraticMessage.trim()" @click="askSocratic">
            {{ isBusy('socratic') ? '…' : '发送' }}
          </button>
        </div>

        <section v-if="socraticSummary" class="mt-4 p-4 rounded-xl border-2 border-green-200 bg-green-50/50">
          <h4 class="font-semibold text-green-800 mb-2">📋 知识点总结卡片 · {{ socraticSummary.topic }}</h4>
          <div v-if="socraticSummary.keyPoints?.length" class="mb-2">
            <p class="text-xs font-medium text-gray-600">核心要点</p>
            <ul class="text-sm list-disc ml-4 text-gray-700">
              <li v-for="(p, i) in socraticSummary.keyPoints" :key="i">{{ p }}</li>
            </ul>
          </div>
          <div v-if="socraticSummary.masteredAspects?.length" class="mb-2">
            <p class="text-xs font-medium text-green-700">掌握较好</p>
            <p class="text-sm text-green-800">{{ socraticSummary.masteredAspects.join('；') }}</p>
          </div>
          <div v-if="socraticSummary.misconceptions?.length" class="mb-2">
            <p class="text-xs font-medium text-red-600">需纠正的误区</p>
            <p class="text-sm text-red-700">{{ socraticSummary.misconceptions.join('；') }}</p>
          </div>
          <p v-if="socraticSummary.logicChainComment" class="text-xs text-gray-600 mb-2">
            逻辑链：{{ socraticSummary.logicChainComment }}
          </p>
          <p v-if="socraticSummary.evaluateScore != null" class="text-sm font-medium text-blue-700 mb-2">
            评估服务 Dubbo 评分：{{ socraticSummary.evaluateScore }} 分（{{ socraticSummary.evaluateGrade }}）· 已存 MongoDB
          </p>
          <p v-if="socraticSummary.recommendedPractice?.length" class="text-xs text-primary-700">
            推荐练习：{{ socraticSummary.recommendedPractice.join('、') }}
          </p>
        </section>
      </section>

      <section class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="font-semibold text-lg">AI 学习周报</h3>
            <p class="text-xs text-gray-400">结构化数据 + ECharts 趋势 + 苏格拉底 MongoDB 记录</p>
          </div>
          <button class="btn-secondary text-sm" :disabled="!!actionLoading" @click="loadReport">
            {{ isBusy('report') ? '生成中…' : '生成周报' }}
          </button>
        </div>

        <template v-if="report">
          <div class="grid sm:grid-cols-3 gap-4 mb-6">
            <div class="rounded-xl bg-gradient-to-br from-primary-50 to-primary-100 p-4">
              <p class="text-xs text-primary-600 mb-1">总积分</p>
              <p class="text-3xl font-bold text-primary-800">{{ report.totalPoints }}</p>
              <div class="mt-2 h-2 bg-white/60 rounded-full overflow-hidden">
                <div class="h-full bg-primary-500 rounded-full transition-all" :style="{ width: `${pointsProgress}%` }" />
              </div>
              <p class="text-xs text-primary-600 mt-1">本周目标 {{ report.weeklyGoalPoints }} 分 · 完成 {{ pointsProgress }}%</p>
            </div>
            <div class="rounded-xl bg-gradient-to-br from-amber-50 to-orange-100 p-4">
              <p class="text-xs text-amber-700 mb-1">连续打卡</p>
              <p class="text-3xl font-bold text-amber-800">{{ report.streakDays }} <span class="text-base font-normal">天</span></p>
              <p class="text-[10px] text-amber-600 mt-1">{{ report.checkedInToday ? '今日已打卡' : '今日未打卡' }}</p>
              <div class="flex gap-1.5 mt-3 justify-center">
                <div
                  v-for="(d, i) in streakWeekDots"
                  :key="i"
                  class="flex flex-col items-center gap-0.5"
                >
                  <div
                    class="w-7 h-7 rounded-full flex items-center justify-center text-xs"
                    :class="d.active ? 'bg-amber-500 text-white shadow-sm' : 'bg-white/70 text-gray-400'"
                  >{{ d.active ? '✓' : '·' }}</div>
                  <span class="text-[10px] text-amber-700">{{ d.day }}</span>
                </div>
              </div>
            </div>
            <div class="rounded-xl bg-gradient-to-br from-green-50 to-emerald-100 p-4">
              <p class="text-xs text-green-700 mb-1">已学知识点</p>
              <p class="text-3xl font-bold text-green-800">{{ report.learnedKnowledgePoints?.length || 0 }}</p>
              <div class="flex flex-wrap gap-1 mt-2">
                <span
                  v-for="kp in (report.weakPoints || []).slice(0, 3)"
                  :key="kp"
                  class="text-[10px] px-1.5 py-0.5 bg-red-100 text-red-700 rounded"
                >弱 {{ kp }}</span>
              </div>
            </div>
          </div>

          <div v-if="knowledgeBars.length" class="mb-6">
            <p class="text-sm font-medium text-gray-700 mb-3">知识点掌握度（AI 练习 + 苏格拉底真实聚合）</p>
            <div class="space-y-2">
              <div v-for="bar in knowledgeBars" :key="bar.name" class="flex items-center gap-3">
                <span class="text-xs text-gray-600 w-28 truncate shrink-0" :title="bar.name">{{ bar.name }}</span>
                <div class="flex-1 h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all"
                    :class="bar.percent >= 80 ? 'bg-green-500' : 'bg-amber-400'"
                    :style="{ width: `${bar.percent}%` }"
                  />
                </div>
                <span class="text-xs w-16 text-right text-gray-400 truncate" :title="bar.source">{{ bar.status }}</span>
              </div>
            </div>
          </div>

          <div v-if="report.pointsTrend?.labels?.length" class="mb-6">
            <p class="text-sm font-medium text-gray-700 mb-2">积分趋势（MongoDB 周快照 · ECharts）</p>
            <div ref="trendChartEl" class="w-full h-48 bg-white rounded-lg border border-gray-100" />
          </div>

          <div v-if="report.recentSocraticSessions?.length" class="mb-6 p-4 bg-purple-50/50 rounded-xl">
            <p class="text-sm font-medium text-purple-800 mb-2">近期苏格拉底（MongoDB）</p>
            <ul class="text-sm space-y-1">
              <li v-for="s in report.recentSocraticSessions" :key="s.id" class="text-gray-700">
                {{ s.topic }}
                <span v-if="s.evaluateScore != null" class="text-purple-600">· {{ s.evaluateScore }}分</span>
                <span class="text-xs text-gray-400">（{{ s.createdAt }}）</span>
              </li>
            </ul>
          </div>

          <div class="rounded-xl border border-gray-100 bg-gray-50/80 p-4">
            <p class="text-xs text-gray-400 mb-2">🤖 AI 学习总结 · {{ report.generatedAt }}</p>
            <div class="text-sm prose-sm max-h-64 overflow-y-auto" v-html="summaryHtml" />
          </div>
        </template>

        <p v-else class="text-sm text-gray-400 text-center py-8">点击「生成周报」查看可视化学习报告</p>
      </section>
    </template>
  </section>
</template>
