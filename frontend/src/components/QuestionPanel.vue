<script setup>import { ref, onMounted, computed, watch } from 'vue';
import { Plus, Search, Edit2, Trash2, HelpCircle, Clock, Flame, Tag, ChevronDown, ChevronUp, X, Save } from 'lucide-vue-next';
import { questionAPI, knowledgePointAPI, evaluationAPI } from '../services/api';
import { extractApiError } from '../utils/extractApiError.js';
import { requireLogin } from '../utils/requireLogin.js';
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
 questions.value = response.data.data || response.data;
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
 questions.value = response.data.data || response.data;
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
 questions.value = response.data.data || response.data;
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
 knowledgePoints.value = response.data.data || response.data;
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
 questions.value = response.data.data || response.data;
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
 await fetchQuestions();
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
        <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索问题..."
          class="w-full sm:w-64 pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
        />
      </div>
      
      <!-- 筛选按钮 -->
      <div class="flex items-center gap-2">
        <div class="flex bg-gray-100 rounded-lg p-1">
          <button
            v-for="filter in [{ id: 'all', name: '全部' }, { id: 'hot', name: '热门' }, { id: 'viewed', name: '常看' }]"
            :key="filter.id"
            @click="switchFilter(filter.id)"
            class="px-3 py-1.5 text-sm font-medium rounded-md transition-all"
            :class="activeFilter === filter.id 
              ? 'bg-white text-primary-600 shadow-sm' 
              : 'text-gray-600 hover:text-gray-900'"
          >
            {{ filter.name }}
          </button>
        </div>
        <button
          @click="openModal()"
          class="flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all duration-200 shadow-md hover:shadow-lg"
        >
          <Plus class="w-5 h-5" />
          <span>提问</span>
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700">
      {{ error }}
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- 问题列表 -->
    <div v-else-if="filteredQuestions.length > 0" class="space-y-4">
      <div
        v-for="question in filteredQuestions"
        :key="question.id"
        class="bg-white rounded-xl border border-gray-200 overflow-hidden hover:shadow-lg transition-all duration-300"
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
                  <h3 class="font-semibold text-gray-900 hover:text-primary-600 transition-colors">
                    {{ question.title }}
                  </h3>
                  <span class="text-xs px-2 py-0.5 bg-primary-50 text-primary-600 rounded-full">
                    {{ getKnowledgePointName(question.knowledgePointId) }}
                  </span>
                </div>
              </div>
              
              <p class="text-gray-600 text-sm ml-13 line-clamp-2 mb-3">{{ question.content }}</p>
              
              <div class="flex items-center gap-4 text-xs text-gray-400">
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
                class="p-2 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                title="编辑"
              >
                <Edit2 class="w-4 h-4" />
              </button>
              <button
                @click.stop="deleteQuestion(question.id)"
                class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                title="删除"
              >
                <Trash2 class="w-4 h-4" />
              </button>
              <component 
                :is="expandedId === question.id ? ChevronUp : ChevronDown" 
                class="w-5 h-5 text-gray-400"
              />
            </div>
          </div>
        </div>

        <!-- 展开详情 -->
        <div v-if="expandedId === question.id" class="px-5 pb-5 border-t border-gray-100 pt-4">
          <div class="bg-gray-50 rounded-xl p-4">
            <h4 class="text-sm font-medium text-gray-700 mb-2">详细描述</h4>
            <p class="text-gray-600 text-sm">{{ question.content }}</p>
          </div>
          <div v-if="question.tags && question.tags.length > 0" class="mt-4">
            <div class="flex items-center gap-1 flex-wrap">
              <Tag class="w-4 h-4 text-gray-400" />
              <span
                v-for="tag in question.tags"
                :key="tag"
                class="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full"
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
      <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <HelpCircle class="w-10 h-10 text-gray-400" />
      </div>
      <h3 class="text-lg font-medium text-gray-900 mb-2">暂无问题</h3>
      <p class="text-gray-500 mb-6">点击上方按钮提出您的第一个问题</p>
      <button
        @click="openModal()"
        class="inline-flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all"
      >
        <Plus class="w-5 h-5" />
        <span>提问</span>
      </button>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg animate-fadeIn">
        <div class="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ isEditing ? '编辑问题' : '提出问题' }}
          </h3>
          <button
            @click="closeModal"
            class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="p-6 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">问题标题 *</label>
            <input
              v-model="currentItem.title"
              type="text"
              placeholder="请输入问题标题"
              class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">所属知识点</label>
            <select
              v-model="currentItem.knowledgePointId"
              class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            >
              <option value="">请选择知识点</option>
              <option v-for="point in knowledgePoints" :key="point.id" :value="point.id">
                {{ point.name }}
              </option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">问题描述</label>
            <textarea
              v-model="currentItem.content"
              rows="4"
              placeholder="请详细描述您的问题"
              class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            ></textarea>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200">
          <button
            @click="closeModal"
            class="px-5 py-2.5 text-gray-600 hover:text-gray-900 font-medium rounded-xl transition-colors"
          >
            取消
          </button>
          <button
            @click="saveQuestion"
            class="flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all"
          >
            <Save class="w-4 h-4" />
            <span>{{ isEditing ? '保存修改' : '提交问题' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
