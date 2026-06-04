<script setup>import { ref, onMounted, computed, watch } from 'vue';
import { Plus, Search, Edit2, Trash2, HelpCircle, Clock, Flame, Tag, ChevronDown, ChevronUp, X, Save } from 'lucide-vue-next';
import { questionAPI, knowledgePointAPI, evaluationAPI, unwrapApiList } from '../services/api';
import { extractApiError } from '../utils/extractApiError.js';
import { requireLogin } from '../utils/requireLogin.js';
import PageLoader from './PageLoader.vue';
const questions = ref([]);
const knowledgePoints = ref([]);
const searchKeyword = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const currentItem = ref({
 title: '',
 content: '',
 knowledgePointId: '',
 tags: []
});
const expandedId = ref(null);
const activeFilter = ref('all');
const loading = ref(true);
const error = ref('');
// 获取所有问题
const fetchQuestions = async () => {
 loading.value = true;
 error.value = '';
 try {
 const response = await questionAPI.getAll();
 questions.value = unwrapApiList(response);
 }
 catch (err) {
 error.value = '获取问题失败: ' + extractApiError(err, '问答服务异常');
 }
 finally {
 loading.value = false;
 }
};
// 热门榜（Redis）
const fetchHotQuestions = async () => {
 loading.value = true;
 error.value = '';
 try {
 const response = await questionAPI.getHot();
 questions.value = unwrapApiList(response);
 }
 catch (err) {
 error.value = '获取热门问题失败: ' + extractApiError(err, 'Redis 热门榜不可用');
 }
 finally {
 loading.value = false;
 }
};
const fetchMostViewedQuestions = async () => {
 loading.value = true;
 error.value = '';
 try {
 const response = await questionAPI.getRecommended();
 questions.value = unwrapApiList(response);
 }
 catch (err) {
 error.value = '获取常看问题失败: ' + extractApiError(err, 'Redis 常看榜不可用');
 }
 finally {
 loading.value = false;
 }
};
const switchFilter = (filterId) => {
 activeFilter.value = filterId;
 if (filterId === 'hot') {
 fetchHotQuestions();
 } else if (filterId === 'viewed') {
 fetchMostViewedQuestions();
 } else {
 fetchQuestions();
 }
};
// 获取知识点列表
const fetchKnowledgePoints = async () => {
 try {
 const response = await knowledgePointAPI.getAll();
 // 兼容两种响应格式：ApiResponse包装格式或直接数组格式
 knowledgePoints.value = unwrapApiList(response);
 }
 catch (err) {
 console.error('获取知识点列表失败:', err);
 }
};
const filteredQuestions = computed(() => questions.value);

let searchDebounceTimer;
watch(searchKeyword, (kw) => {
 clearTimeout(searchDebounceTimer);
 searchDebounceTimer = setTimeout(async () => {
 const trimmed = (kw || '').trim();
 if (!trimmed) {
 if (activeFilter.value === 'hot') await fetchHotQuestions();
 else if (activeFilter.value === 'viewed') await fetchMostViewedQuestions();
 else await fetchQuestions();
 return;
 }
 loading.value = true;
 error.value = '';
 try {
 const response = await questionAPI.search(trimmed);
 questions.value = unwrapApiList(response);
 } catch (err) {
 error.value = '搜索失败: ' + extractApiError(err, '搜索不可用');
 } finally {
 loading.value = false;
 }
 }, 350);
});
// 获取知识点名称
const getKnowledgePointName = (id) => {
 const point = knowledgePoints.value.find(p => p.id === id);
 return point ? point.name : '未知';
};
// 打开创建/编辑模态框
const openModal = (item = null) => {
 if (!requireLogin()) return;
 if (item) {
 isEditing.value = true;
 currentItem.value = { ...item };
 }
 else {
 isEditing.value = false;
 currentItem.value = { title: '', content: '', knowledgePointId: '', tags: [] };
 }
 showModal.value = true;
};
// 关闭模态框
const closeModal = () => {
 showModal.value = false;
 currentItem.value = { title: '', content: '', knowledgePointId: '', tags: [] };
};
// 仅提交后端认识的字段（避免 tags 等导致 500）
const buildQuestionPayload = () => {
 const title = currentItem.value.title.trim();
 const content = (currentItem.value.content || title).trim();
 return {
 title,
 content,
 knowledgePointId: currentItem.value.knowledgePointId || undefined
 };
};
// 保存问题
const saveQuestion = async () => {
 if (!currentItem.value.title.trim()) {
 alert('请输入问题标题');
 return;
 }
 if (!currentItem.value.knowledgePointId) {
 alert('请选择所属知识点');
 return;
 }
 const payload = buildQuestionPayload();
 try {
 let response;
 if (isEditing.value) {
 response = await questionAPI.update(currentItem.value.id, payload);
 }
 else {
 response = await questionAPI.create(payload);
 }
 const created = response.data?.data ?? response.data;
 closeModal();
 await switchFilter(activeFilter.value);
 }
 catch (err) {
 alert('保存失败: ' + (err.response?.data?.message || err.message));
 }
};
// 删除问题
const deleteQuestion = async (id) => {
 if (!requireLogin()) return;
 if (!confirm('确定要删除这个问题吗？'))
 return;
 try {
 await questionAPI.delete(id);
 try {
 await evaluationAPI.deleteByQuestionId(id);
 } catch (cleanupErr) {
 console.warn('评估记录同步清理:', cleanupErr);
 }
 await fetchQuestions();
 }
 catch (err) {
 alert('删除失败: ' + (err.response?.data?.message || err.message));
 }
};
// 格式化时间
const formatDate = (dateString) => {
 if (!dateString)
 return '';
 const date = new Date(dateString);
 return date.toLocaleString('zh-CN', {
 year: 'numeric',
 month: '2-digit',
 day: '2-digit',
 hour: '2-digit',
 minute: '2-digit'
 });
};
// 展开详情时调用 GET /questions/{id}，浏览量 +1（后端 MySQL + Redis）
const toggleExpand = async (id) => {
 if (expandedId.value === id) {
 expandedId.value = null;
 return;
 }
 expandedId.value = id;
 try {
 const response = await questionAPI.getById(id);
 const detail = response.data?.data ?? response.data;
 if (detail) {
 const idx = questions.value.findIndex((q) => q.id === id);
 if (idx >= 0) {
 questions.value[idx] = { ...questions.value[idx], ...detail };
 }
 }
 } catch (err) {
 console.warn('记录浏览失败:', err);
 }
};
onMounted(() => {
 fetchQuestions();
 fetchKnowledgePoints();
});
</script>

<template>
  <div class="animate-fadeIn">
    <!-- 头部操作区 -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
      <div class="relative flex-1 w-full sm:w-auto">
        <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-faint" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索问题..."
          class="input input-sm w-full sm:w-64 pl-10"
        />
      </div>
      
      <!-- 筛选按钮 -->
      <div class="flex items-center gap-2">
        <div class="segment">
          <button
            v-for="filter in [{ id: 'all', name: '全部' }, { id: 'hot', name: '热门' }, { id: 'viewed', name: '常看' }]"
            :key="filter.id"
            @click="switchFilter(filter.id)"
            class="segment-item"
            :class="{ 'segment-item-active': activeFilter === filter.id }"
          >
            {{ filter.name }}
          </button>
        </div>
        <button
          @click="openModal()"
          class="btn-primary flex items-center gap-2"
        >
          <Plus class="w-5 h-5" />
          <span>提问</span>
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="alert-error">
      {{ error }}
    </div>

    <!-- 加载状态 -->
    <PageLoader v-if="loading" label="正在加载题目…" />

    <!-- 问题列表 -->
    <div v-else-if="filteredQuestions.length > 0" class="space-y-4">
      <div
        v-for="question in filteredQuestions"
        :key="question.id"
        class="list-card overflow-hidden !p-0"
      >
        <!-- 问题头部 -->
        <div class="p-5 cursor-pointer" @click="toggleExpand(question.id)">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <div class="w-10 h-10 bg-amber-100 rounded-lg flex items-center justify-center">
                  <HelpCircle class="w-5 h-5 text-amber-600" />
                </div>
                <div>
                  <h3 class="font-semibold text-main hover:text-primary-600 transition-colors">
                    {{ question.title }}
                  </h3>
                  <span class="text-xs px-2 py-0.5 bg-primary-50 text-primary-600 rounded-full">
                    {{ getKnowledgePointName(question.knowledgePointId) }}
                  </span>
                </div>
              </div>
              
              <p class="text-muted text-sm ml-13 line-clamp-2 mb-3">{{ question.content }}</p>
              
              <div class="flex items-center gap-4 text-xs text-faint">
                <div class="flex items-center gap-1">
                  <Clock class="w-3 h-3" />
                  <span>{{ formatDate(question.createdAt) }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <Flame class="w-3 h-3" />
                  <span>{{ question.viewCount || 0 }} 浏览</span>
                </div>
                <span>{{ question.answerCount || 0 }} 回答</span>
              </div>
            </div>
            
            <div class="flex items-center gap-2">
              <button
                @click.stop="openModal(question)"
                class="icon-btn"
                title="编辑"
              >
                <Edit2 class="w-4 h-4" />
              </button>
              <button
                @click.stop="deleteQuestion(question.id)"
                class="p-2 text-faint hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                title="删除"
              >
                <Trash2 class="w-4 h-4" />
              </button>
              <component 
                :is="expandedId === question.id ? ChevronUp : ChevronDown" 
                class="w-5 h-5 text-faint"
              />
            </div>
          </div>
        </div>

        <!-- 展开详情 -->
        <div v-if="expandedId === question.id" class="px-5 pb-5 border-t border-gray-100 pt-4">
          <div class="subtle-panel">
            <h4 class="text-sm font-medium text-muted mb-2">详细描述</h4>
            <p class="text-muted text-sm">{{ question.content }}</p>
          </div>
          <div v-if="question.tags && question.tags.length > 0" class="mt-4">
            <div class="flex items-center gap-1 flex-wrap">
              <Tag class="w-4 h-4 text-faint" />
              <span
                v-for="tag in question.tags"
                :key="tag"
                class="badge"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-16">
      <div class="w-20 h-20 bg-muted rounded-full flex items-center justify-center mx-auto mb-4">
        <HelpCircle class="w-10 h-10 text-faint" />
      </div>
      <h3 class="text-lg font-medium text-main mb-2">暂无问题</h3>
      <p class="text-muted mb-6">点击上方按钮提出您的第一个问题</p>
      <button
        @click="openModal()"
        class="btn-primary inline-flex items-center gap-2"
      >
        <Plus class="w-5 h-5" />
        <span>提问</span>
      </button>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="modal-backdrop z-50">
      <div class="modal-card max-w-lg">
        <div class="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-main">
            {{ isEditing ? '编辑问题' : '提出问题' }}
          </h3>
          <button
            @click="closeModal"
            class="icon-btn"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-muted mb-2">问题标题 *</label>
            <input
              v-model="currentItem.title"
              type="text"
              placeholder="请输入问题标题"
              class="input"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-muted mb-2">所属知识点</label>
            <select
              v-model="currentItem.knowledgePointId"
              class="input"
            >
              <option value="">请选择知识点</option>
              <option v-for="point in knowledgePoints" :key="point.id" :value="point.id">
                {{ point.name }}
              </option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-muted mb-2">问题描述</label>
            <textarea
              v-model="currentItem.content"
              rows="4"
              placeholder="请详细描述您的问题"
              class="input"
            ></textarea>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200">
          <button
            @click="closeModal"
            class="px-5 py-2.5 text-muted hover:text-main font-medium rounded-xl transition-colors"
          >
            取消
          </button>
          <button
            @click="saveQuestion"
            class="btn-primary flex items-center gap-2"
          >
            <Save class="w-4 h-4" />
            <span>{{ isEditing ? '保存修改' : '提交问题' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
