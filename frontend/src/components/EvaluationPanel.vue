<script setup>
import { ref, onMounted, computed } from 'vue';
import { Star, Brain, TrendingUp, Filter, RefreshCw, Clock, Award, Target } from 'lucide-vue-next';
import { evaluationAPI, questionAPI, unwrapApiList } from '../services/api';
import { extractApiError } from '../utils/extractApiError.js';
import { formatShortId } from '../utils/formatId';
import PageLoader from './PageLoader.vue';

const questionEvaluations = ref([]);
const answerEvaluations = ref([]);
const activeTab = ref('question');
const difficultyFilter = ref('ALL');
const gradeFilter = ref('ALL');
const loading = ref(true);
const error = ref('');
const reEvaluating = ref(false);
const questionTitleById = ref({});

const loadQuestionTitles = async () => {
  try {
    const response = await questionAPI.getAll();
    const list = unwrapApiList(response);
    const map = {};
    for (const q of list) {
      if (q?.id) map[q.id] = q.title;
    }
    questionTitleById.value = map;
  } catch (err) {
    console.warn('加载问题标题失败:', err);
  }
};

const getQuestionTitle = (questionId) =>
  questionTitleById.value[questionId] || '（原问题已删除）';

/** 同一问题/回答只保留最新一条评估记录 */
const dedupeLatest = (list, idKey) => {
  const map = new Map();
  for (const item of list) {
    const key = item[idKey];
    if (!key) continue;
    const prev = map.get(key);
    const t = item.createdAt ? new Date(item.createdAt).getTime() : 0;
    const pt = prev?.createdAt ? new Date(prev.createdAt).getTime() : 0;
    if (!prev || t >= pt) map.set(key, item);
  }
  return [...map.values()];
};

const pruneOrphanEvaluations = async () => {
  const known = new Set(Object.keys(questionTitleById.value));
  if (known.size === 0) return;
  const orphanQuestionIds = [
    ...new Set(
      questionEvaluations.value
        .filter((e) => e.questionId && !known.has(e.questionId))
        .map((e) => e.questionId)
    )
  ];
  for (const questionId of orphanQuestionIds) {
    try {
      await evaluationAPI.deleteByQuestionId(questionId);
    } catch (err) {
      console.warn('清理孤立评估失败', questionId, err);
    }
  }
  if (orphanQuestionIds.length > 0) {
    const response = await evaluationAPI.getAllQuestionEvaluations();
    const list = unwrapApiList(response);
    questionEvaluations.value = (list || []).map((item) => ({
      ...item,
      knowledgePoints: parseStringList(item.knowledgePoints)
    }));
  }
};

const fetchQuestionEvaluations = async () => {
  loading.value = true;
  error.value = '';
  try {
    const response = await evaluationAPI.getAllQuestionEvaluations();
    const list = unwrapApiList(response);
    questionEvaluations.value = (list || []).map(item => ({
      ...item,
      knowledgePoints: parseStringList(item.knowledgePoints)
    }));
    await pruneOrphanEvaluations();
  } catch (err) {
    error.value = '获取问题评估失败: ' + extractApiError(err, '评估服务异常');
  } finally {
    loading.value = false;
  }
};

const fetchAnswerEvaluations = async () => {
  loading.value = true;
  error.value = '';
  try {
    const response = await evaluationAPI.getAllAnswerEvaluations();
    const list = unwrapApiList(response);
    answerEvaluations.value = (list || []).map(item => ({
      ...item,
      strengths: parseStringList(item.strengths),
      suggestions: parseStringList(item.suggestions)
    }));
  } catch (err) {
    error.value = '获取答案评估失败: ' + extractApiError(err, '评估服务异常');
  } finally {
    loading.value = false;
  }
};

const filteredQuestionEvaluations = computed(() => {
  const knownIds = Object.keys(questionTitleById.value);
  let list = dedupeLatest(questionEvaluations.value, 'questionId');
  if (knownIds.length > 0) {
    list = list.filter((item) => knownIds.includes(item.questionId));
  }
  if (difficultyFilter.value === 'ALL') return list;
  return list.filter((item) => item.difficulty === difficultyFilter.value);
});

const filteredAnswerEvaluations = computed(() => {
  const knownIds = Object.keys(questionTitleById.value);
  let list = dedupeLatest(answerEvaluations.value, 'answerId');
  if (knownIds.length > 0) {
    list = list.filter((item) => knownIds.includes(item.questionId));
  }
  if (gradeFilter.value === 'ALL') return list;
  return list.filter((item) => item.grade === gradeFilter.value);
});

const getDifficultyColor = (difficulty) => {
  switch (difficulty) {
    case 'EASY': return 'bg-green-100 text-green-700';
    case 'MEDIUM': return 'bg-yellow-100 text-yellow-700';
    case 'HARD': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-muted';
  }
};

const getGradeColor = (grade) => {
  switch (grade) {
    case 'A': return 'bg-emerald-100 text-emerald-700';
    case 'B': return 'bg-blue-100 text-blue-700';
    case 'C': return 'bg-yellow-100 text-yellow-700';
    case 'D': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-muted';
  }
};

const getScoreColor = (score) => {
  if (score >= 90) return 'text-emerald-600';
  if (score >= 70) return 'text-blue-600';
  if (score >= 60) return 'text-yellow-600';
  return 'text-red-600';
};

/** 兼容 API 返回 JSON 字符串或数组 */
const parseStringList = (value) => {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed);
        return Array.isArray(parsed) ? parsed : [trimmed];
      } catch {
        return [trimmed];
      }
    }
    return [trimmed];
  }
  return [];
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const refreshData = async () => {
  await loadQuestionTitles();
  if (activeTab.value === 'question') {
    await fetchQuestionEvaluations();
  } else {
    await fetchAnswerEvaluations();
  }
};

onMounted(async () => {
  await loadQuestionTitles();
  await Promise.all([fetchQuestionEvaluations(), fetchAnswerEvaluations()]);
});
</script>

<template>
  <div class="animate-fadeIn">
    <!-- Tab切换 -->
    <div class="flex items-center justify-between mb-6">
      <div class="segment">
        <button
          @click="activeTab = 'question'; refreshData()"
          class="segment-item flex items-center gap-2"
          :class="{ 'segment-item-active': activeTab === 'question' }"
        >
          <Brain class="w-4 h-4" />
          <span>问题评估</span>
        </button>
        <button
          @click="activeTab = 'answer'; refreshData()"
          class="segment-item flex items-center gap-2"
          :class="{ 'segment-item-active': activeTab === 'answer' }"
        >
          <Star class="w-4 h-4" />
          <span>答案评分</span>
        </button>
      </div>
      <button
        @click="refreshData"
        class="btn-primary flex items-center gap-2"
      >
        <RefreshCw class="w-4 h-4" />
        <span>刷新</span>
      </button>
    </div>

    <!-- 筛选器 -->
    <div v-if="activeTab === 'question'" class="flex items-center gap-4 mb-6">
      <div class="flex items-center gap-2">
        <Filter class="w-4 h-4 text-faint" />
        <span class="text-sm text-muted">难度筛选:</span>
      </div>
      <div class="flex gap-2">
        <button
          v-for="level in ['ALL', 'EASY', 'MEDIUM', 'HARD']"
          :key="level"
          @click="difficultyFilter = level"
          class="chip"
          :class="{ 'chip-active': difficultyFilter === level }"
        >
          {{ level === 'ALL' ? '全部' : level === 'EASY' ? '简单' : level === 'MEDIUM' ? '中等' : '困难' }}
        </button>
      </div>
    </div>

    <div v-else class="flex items-center gap-4 mb-6">
      <div class="flex items-center gap-2">
        <Filter class="w-4 h-4 text-faint" />
        <span class="text-sm text-muted">等级筛选:</span>
      </div>
      <div class="flex gap-2">
        <button
          v-for="grade in ['ALL', 'A', 'B', 'C', 'D']"
          :key="grade"
          @click="gradeFilter = grade"
          class="chip"
          :class="{ 'chip-active': gradeFilter === grade }"
        >
          {{ grade === 'ALL' ? '全部' : grade }}
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="alert-error flex items-center justify-between gap-4">
      <span>{{ error }}</span>
      <button
        @click="activeTab === 'question' ? fetchQuestionEvaluations() : fetchAnswerEvaluations()"
        class="shrink-0 px-4 py-1.5 text-sm font-medium bg-red-100 hover:bg-red-200 rounded-lg transition-colors"
      >
        重试
      </button>
    </div>

    <!-- 加载状态 -->
    <PageLoader v-if="loading" label="正在加载评测…" />

    <!-- 问题评估列表 -->
    <div v-else-if="activeTab === 'question' && filteredQuestionEvaluations.length > 0" class="space-y-4">
      <div
        v-for="item in filteredQuestionEvaluations"
        :key="item.id"
        class="list-card"
      >
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center">
              <Brain class="w-6 h-6 text-primary-600" />
            </div>
            <div>
              <h3 class="font-semibold text-main">{{ item.questionTitle }}</h3>
              <p class="text-xs text-faint" :title="item.questionId">编号 {{ formatShortId(item.questionId) }}</p>
            </div>
          </div>
          <span 
            class="px-3 py-1 rounded-full text-sm font-medium"
            :class="getDifficultyColor(item.difficulty)"
          >
            {{ item.difficulty === 'EASY' ? '简单' : item.difficulty === 'MEDIUM' ? '中等' : '困难' }}
          </span>
        </div>
        
        <div class="mb-4">
          <p class="text-sm text-muted mb-2">
            <span class="font-medium">评估详情:</span> {{ item.evaluationDetails || '无' }}
          </p>
        </div>

        <div v-if="item.knowledgePoints && item.knowledgePoints.length > 0" class="mb-4">
          <div class="flex items-center gap-2 mb-2">
            <Target class="w-4 h-4 text-primary-500" />
            <span class="text-sm font-medium text-muted">知识点:</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(point, index) in item.knowledgePoints"
              :key="index"
              class="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded-full"
            >
              {{ point }}
            </span>
          </div>
        </div>

        <div class="flex items-center justify-between text-xs text-faint">
          <div class="flex items-center gap-1">
            <Clock class="w-3 h-3" />
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 答案评估列表 -->
    <div v-else-if="activeTab === 'answer' && filteredAnswerEvaluations.length > 0" class="space-y-4">
      <div
        v-for="item in filteredAnswerEvaluations"
        :key="item.id"
        class="list-card"
      >
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
              <Award class="w-6 h-6 text-yellow-600" />
            </div>
            <div>
              <p class="font-medium text-main">{{ getQuestionTitle(item.questionId) }}</p>
              <p class="text-xs text-faint" :title="`答案 ${item.answerId} / 问题 ${item.questionId}`">
                答案编号 {{ formatShortId(item.answerId) }}
              </p>
            </div>
          </div>
          <div class="text-right">
            <div class="text-2xl font-bold" :class="getScoreColor(item.score)">
              {{ item.score }}分
            </div>
            <span 
              class="px-3 py-1 rounded-full text-sm font-medium"
              :class="getGradeColor(item.grade)"
            >
              {{ item.grade }}级
            </span>
          </div>
        </div>

        <div class="mb-4 subtle-panel">
          <p class="text-sm text-muted mb-2">
            <span class="font-medium">答案内容:</span> {{ item.answerContent }}
          </p>
        </div>

        <div v-if="item.strengths && item.strengths.length > 0" class="mb-4">
          <div class="flex items-center gap-2 mb-2">
            <TrendingUp class="w-4 h-4 text-emerald-500" />
            <span class="text-sm font-medium text-muted">优点:</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(strength, index) in item.strengths"
              :key="index"
              class="px-2 py-1 bg-emerald-50 text-emerald-700 text-xs rounded-full"
            >
              {{ strength }}
            </span>
          </div>
        </div>

        <div v-if="item.suggestions && item.suggestions.length > 0" class="mb-4">
          <div class="flex items-center gap-2 mb-2">
            <Target class="w-4 h-4 text-blue-500" />
            <span class="text-sm font-medium text-muted">建议:</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(suggestion, index) in item.suggestions"
              :key="index"
              class="px-2 py-1 bg-blue-50 text-blue-700 text-xs rounded-full"
            >
              {{ suggestion }}
            </span>
          </div>
        </div>

        <div class="flex items-center justify-between text-xs text-faint">
          <div class="flex items-center gap-1">
            <Clock class="w-3 h-3" />
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-16">
      <div class="w-20 h-20 bg-muted rounded-full flex items-center justify-center mx-auto mb-4">
        <Brain class="w-10 h-10 text-faint" />
      </div>
      <h3 class="text-lg font-medium text-main mb-2">
        {{ activeTab === 'question' ? '暂无问题评估' : '暂无答案评分' }}
      </h3>
      <p class="text-muted mb-6">
        {{ activeTab === 'question' ? '在问题管理中创建问题即可自动进行评估' : '在回答管理中创建回答即可自动进行评分' }}
      </p>
    </div>
  </div>
</template>