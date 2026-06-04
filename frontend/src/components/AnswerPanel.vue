<script setup>import { ref, onMounted, computed } from 'vue';
import { Plus, Search, Edit2, Trash2, MessageSquare, Clock, ThumbsUp, MessageCircle, ChevronDown, ChevronUp, X, Save, Send, Star } from 'lucide-vue-next';
import { answerAPI, questionAPI, knowledgePointAPI, unwrapApiList } from '../services/api';
import { extractApiError } from '../utils/extractApiError.js';
import { requireLogin } from '../utils/requireLogin.js';
import PageLoader from './PageLoader.vue';
const answers = ref([]);
const questions = ref([]);
const knowledgePoints = ref([]);
const searchKeyword = ref('');
const showModal = ref(false);
const showCommentModal = ref(false);
const isEditing = ref(false);
const currentItem = ref({
 content: '',
 questionId: ''
});
const currentAnswerId = ref('');
const newComment = ref('');
const expandedId = ref(null);
const activeFilter = ref('all');
const loading = ref(true);
const error = ref('');

// 获取所有回答
const fetchAnswers = async () => {
 loading.value = true;
 error.value = '';
 try {
 const response = await answerAPI.getAll();
 // 兼容两种响应格式：ApiResponse包装格式或直接数组格式
 answers.value = unwrapApiList(response);
 }
 catch (err) {
 error.value = '获取回答失败: ' + extractApiError(err, '问答服务异常');
 }
 finally {
 loading.value = false;
 }
};
// 获取问题列表
const fetchQuestions = async () => {
 try {
 const response = await questionAPI.getAll();
 // 兼容两种响应格式：ApiResponse包装格式或直接数组格式
 questions.value = unwrapApiList(response);
 }
 catch (err) {
 console.error('获取问题列表失败:', err);
 }
};
// 搜索过滤
const filteredAnswers = computed(() => {
 let result = answers.value;
 // 关键词搜索
 if (searchKeyword.value) {
 const keyword = searchKeyword.value.toLowerCase();
 result = result.filter(item => item.content.toLowerCase().includes(keyword));
 }
 // 分类筛选
 if (activeFilter.value === 'hot') {
 result = [...result].filter((a) => (a.likeCount || 0) > 0)
   .sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
 }
 return result;
});
// 获取问题标题
const getQuestionTitle = (id) => {
 const question = questions.value.find(q => q.id === id);
 return question ? question.title : '未知问题';
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
 currentItem.value = { content: '', questionId: '' };
 }
 showModal.value = true;
};
// 关闭模态框
const closeModal = () => {
 showModal.value = false;
 currentItem.value = { content: '', questionId: '' };
};
// 保存回答
const saveAnswer = async () => {
 if (!currentItem.value.content.trim()) {
 alert('请输入回答内容');
 return;
 }
 if (!currentItem.value.questionId) {
 alert('请选择问题');
 return;
 }
 try {
 if (isEditing.value) {
 await answerAPI.update(currentItem.value.id, currentItem.value);
 }
 else {
 await answerAPI.create({ ...currentItem.value });
 }
 closeModal();
 await fetchAnswers();
 }
 catch (err) {
 alert('保存失败: ' + (err.response?.data?.message || err.message));
 }
};
// 删除回答
const deleteAnswer = async (id) => {
 if (!requireLogin()) return;
 if (!confirm('确定要删除这个回答吗？'))
 return;
 try {
 await answerAPI.delete(id);
 await fetchAnswers();
 }
 catch (err) {
 alert('删除失败: ' + (err.response?.data?.message || err.message));
 }
};
// 点赞回答
const likeAnswer = async (id) => {
 if (!requireLogin()) return;
 try {
 await answerAPI.like(id);
 await fetchAnswers();
 }
 catch (err) {
 alert('点赞失败: ' + (err.response?.data?.message || err.message));
 }
};
// 打开评论模态框
const openCommentModal = (answerId) => {
 if (!requireLogin()) return;
 currentAnswerId.value = answerId;
 newComment.value = '';
 showCommentModal.value = true;
};
// 关闭评论模态框
const closeCommentModal = () => {
 showCommentModal.value = false;
 currentAnswerId.value = '';
 newComment.value = '';
};
// 添加评论
const addComment = async () => {
 if (!newComment.value.trim()) {
 alert('请输入评论内容');
 return;
 }
 try {
 await answerAPI.comment(currentAnswerId.value, { content: newComment.value });
 closeCommentModal();
 await fetchAnswers();
 }
 catch (err) {
 alert('添加评论失败: ' + (err.response?.data?.message || err.message));
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
// 切换展开状态
const toggleExpand = (id) => {
 expandedId.value = expandedId.value === id ? null : id;
};
onMounted(() => {
 fetchAnswers();
 fetchQuestions();
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
          placeholder="搜索回答内容..."
          class="input input-sm w-full sm:w-64 pl-10"
        />
      </div>
      
      <!-- 筛选按钮 -->
      <div class="flex items-center gap-2">
        <div class="segment">
          <button
            v-for="filter in [{ id: 'all', name: '全部' }, { id: 'hot', name: '高赞' }]"
            :key="filter.id"
            @click="activeFilter = filter.id"
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
          <span>回答问题</span>
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="alert-error flex items-center justify-between gap-4">
      <span>{{ error }}</span>
      <button
        @click="fetchAnswers"
        class="shrink-0 px-4 py-1.5 text-sm font-medium bg-red-100 hover:bg-red-200 rounded-lg transition-colors"
      >
        重试
      </button>
    </div>

    <!-- 加载状态 -->
    <PageLoader v-if="loading" label="正在加载回答…" />

    <!-- 回答列表 -->
    <div v-else-if="filteredAnswers.length > 0" class="space-y-4">
      <div
        v-for="answer in filteredAnswers"
        :key="answer.id"
        class="list-card overflow-hidden !p-0"
      >
        <!-- 回答头部 -->
        <div class="p-5 cursor-pointer" @click="toggleExpand(answer.id)">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                  <MessageSquare class="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <h3 class="font-semibold text-main hover:text-primary-600 transition-colors">
                    回答: {{ getQuestionTitle(answer.questionId) }}
                  </h3>
                  <span class="text-xs text-muted">问题ID: {{ answer.questionId }}</span>
                </div>
              </div>
              
              <p class="text-muted text-sm ml-13 line-clamp-2 mb-3">{{ answer.content }}</p>
              
              <div class="flex items-center gap-4 text-xs text-faint">
                <div class="flex items-center gap-1">
                  <Clock class="w-3 h-3" />
                  <span>{{ formatDate(answer.createdAt) }}</span>
                </div>
                <button
                  @click.stop="likeAnswer(answer.id)"
                  class="flex items-center gap-1 text-amber-500 hover:text-amber-600 transition-colors"
                >
                  <ThumbsUp class="w-3 h-3" />
                  <span>{{ answer.likeCount || 0 }}</span>
                </button>
                <button
                  @click.stop="openCommentModal(answer.id)"
                  class="flex items-center gap-1 text-blue-500 hover:text-blue-600 transition-colors"
                >
                  <MessageCircle class="w-3 h-3" />
                  <span>{{ answer.comments?.length || 0 }}</span>
                </button>
              </div>
            </div>
            
            <div class="flex items-center gap-2">
              <button
                @click.stop="openModal(answer)"
                class="p-2 text-faint hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                title="编辑"
              >
                <Edit2 class="w-4 h-4" />
              </button>
              <button
                @click.stop="deleteAnswer(answer.id)"
                class="p-2 text-faint hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                title="删除"
              >
                <Trash2 class="w-4 h-4" />
              </button>
              <component 
                :is="expandedId === answer.id ? ChevronUp : ChevronDown" 
                class="w-5 h-5 text-faint"
              />
            </div>
          </div>
        </div>

        <!-- 展开详情 -->
        <div v-if="expandedId === answer.id" class="px-5 pb-5 border-t border-gray-100 pt-4">
          <div class="subtle-panel">
            <h4 class="text-sm font-medium text-muted mb-2">完整回答</h4>
            <p class="text-muted text-sm whitespace-pre-wrap">{{ answer.content }}</p>
          </div>
          
          <!-- 评论列表 -->
          <div v-if="answer.comments && answer.comments.length > 0" class="mt-4">
            <h4 class="text-sm font-medium text-muted mb-3">评论 ({{ answer.comments.length }})</h4>
            <div class="space-y-3">
              <div
                v-for="(comment, index) in answer.comments"
                :key="index"
                class="subtle-panel !p-3"
              >
                <p class="text-muted text-sm">{{ comment.content }}</p>
                <span class="text-xs text-faint mt-1 block">{{ formatDate(comment.createdAt) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 添加评论按钮 -->
          <button
            @click.stop="openCommentModal(answer.id)"
            class="mt-4 flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 transition-colors"
          >
            <MessageCircle class="w-4 h-4" />
            <span>添加评论</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-16">
      <div class="w-20 h-20 bg-muted rounded-full flex items-center justify-center mx-auto mb-4">
        <MessageSquare class="w-10 h-10 text-faint" />
      </div>
      <h3 class="text-lg font-medium text-main mb-2">暂无回答</h3>
      <p class="text-muted mb-6">点击上方按钮回答问题</p>
      <button
        @click="openModal()"
        class="btn-primary inline-flex items-center gap-2"
      >
        <Plus class="w-5 h-5" />
        <span>回答问题</span>
      </button>
    </div>

    <!-- 创建/编辑模态框 -->
    <div v-if="showModal" class="modal-backdrop z-50">
      <div class="modal-card max-w-lg">
        <div class="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-main">
            {{ isEditing ? '编辑回答' : '回答问题' }}
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
            <label class="block text-sm font-medium text-muted mb-2">选择问题 *</label>
            <select
              v-model="currentItem.questionId"
              class="input"
            >
              <option value="">请选择问题</option>
              <option v-for="question in questions" :key="question.id" :value="question.id">
                {{ question.title }}
              </option>
            </select>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-muted mb-2">回答内容 *</label>
            <textarea
              v-model="currentItem.content"
              rows="6"
              placeholder="请输入您的回答"
              class="input"
            ></textarea>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200">
          <button
            @click="closeModal"
            class="btn-secondary"
          >
            取消
          </button>
          <button
            @click="saveAnswer"
            class="btn-primary flex items-center gap-2"
          >
            <Save class="w-4 h-4" />
            <span>{{ isEditing ? '保存修改' : '提交回答' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 评论模态框 -->
    <div v-if="showCommentModal" class="modal-backdrop z-50">
      <div class="modal-card max-w-md">
        <div class="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-main">添加评论</h3>
          <button
            @click="closeCommentModal"
            class="icon-btn"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="p-6">
          <textarea
            v-model="newComment"
            rows="4"
            placeholder="请输入评论内容..."
            class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none mb-4"
          ></textarea>
        </div>
        
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200">
          <button
            @click="closeCommentModal"
            class="btn-secondary"
          >
            取消
          </button>
          <button
            @click="addComment"
            class="btn-primary flex items-center gap-2"
          >
            <Send class="w-4 h-4" />
            <span>发表评论</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
