<script setup>import { ref, onMounted } from 'vue';
import { Plus, Search, Edit2, Trash2, Eye, Save, X, BookOpen, Clock } from 'lucide-vue-next';
import { knowledgePointAPI, questionAPI, unwrapApiList } from '../services/api';
import { requireLogin } from '../utils/requireLogin.js';
import PageLoader from './PageLoader.vue';
const knowledgePoints = ref([]);
const searchKeyword = ref('');
const showModal = ref(false);
const isEditing = ref(false);
const currentItem = ref({
 name: '',
 description: '',
 path: ''
});
const loading = ref(true);
const error = ref('');
// 获取所有知识点
const fetchKnowledgePoints = async () => {
 loading.value = true;
 error.value = '';
 try {
 const [kpRes, qRes] = await Promise.all([
   knowledgePointAPI.getAll(),
   questionAPI.getAll()
 ]);
 const points = unwrapApiList(kpRes);
 const questions = unwrapApiList(qRes);
 const countByKp = {};
 questions.forEach((q) => {
   if (q.knowledgePointId) {
     countByKp[q.knowledgePointId] = (countByKp[q.knowledgePointId] || 0) + 1;
   }
 });
 knowledgePoints.value = points.map((p) => ({
   ...p,
   questionCount: countByKp[p.id] ?? p.questionCount ?? 0
 }));
 }
 catch (err) {
 error.value = '获取知识点失败: ' + (err.response?.data?.message || err.message);
 }
 finally {
 loading.value = false;
 }
};
// 搜索过滤
const filteredPoints = () => {
 if (!searchKeyword.value)
 return knowledgePoints.value;
 const keyword = searchKeyword.value.toLowerCase();
 return knowledgePoints.value.filter(item => item.name.toLowerCase().includes(keyword) ||
 item.description.toLowerCase().includes(keyword) ||
 item.path.toLowerCase().includes(keyword));
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
 currentItem.value = { name: '', description: '', path: '' };
 }
 showModal.value = true;
};
// 关闭模态框
const closeModal = () => {
 showModal.value = false;
 currentItem.value = { name: '', description: '', path: '' };
};
// 保存知识点
const saveKnowledgePoint = async () => {
 if (!currentItem.value.name.trim()) {
 alert('请输入知识点名称');
 return;
 }
 try {
 if (isEditing.value) {
 await knowledgePointAPI.update(currentItem.value.id, currentItem.value);
 }
 else {
 await knowledgePointAPI.create(currentItem.value);
 }
 closeModal();
 await fetchKnowledgePoints();
 }
 catch (err) {
 alert('保存失败: ' + (err.response?.data?.message || err.message));
 }
};
// 删除知识点
const deleteKnowledgePoint = async (id) => {
 if (!requireLogin()) return;
 if (!confirm('确定要删除这个知识点吗？'))
 return;
 try {
 await knowledgePointAPI.delete(id);
 await fetchKnowledgePoints();
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
onMounted(() => {
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
          placeholder="搜索知识点..."
          class="input input-sm w-full sm:w-64 pl-10"
        />
      </div>
      <button
        @click="openModal()"
        class="btn-primary flex items-center gap-2"
      >
        <Plus class="w-5 h-5" />
        <span>新建知识点</span>
      </button>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="alert-error">
      {{ error }}
    </div>

    <!-- 加载状态 -->
    <PageLoader v-if="loading" label="正在加载知识点…" />

    <!-- 知识点列表 -->
    <div v-else-if="filteredPoints().length > 0" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="point in filteredPoints()"
        :key="point.id"
        class="list-card group"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-2">
            <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
              <BookOpen class="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <h3 class="font-semibold text-main group-hover:text-primary-600 transition-colors">
                {{ point.name }}
              </h3>
              <span v-if="point.path" class="badge">
                {{ point.path }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              @click="openModal(point)"
              class="icon-btn"
              title="编辑"
            >
              <Edit2 class="w-4 h-4" />
            </button>
            <button
              @click="deleteKnowledgePoint(point.id)"
              class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              title="删除"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>
        
        <p class="text-muted text-sm mb-4 line-clamp-2">{{ point.description }}</p>
        
        <div class="flex items-center justify-between text-xs text-faint">
          <div class="flex items-center gap-1">
            <Clock class="w-3 h-3" />
            <span>{{ formatDate(point.createdAt) }}</span>
          </div>
          <span>{{ point.questionCount || 0 }} 个问题</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="w-20 h-20 bg-muted rounded-full flex items-center justify-center mx-auto mb-4">
        <BookOpen class="w-10 h-10 text-faint" />
      </div>
      <h3 class="text-lg font-medium text-main mb-2">暂无知识点</h3>
      <p class="text-muted mb-6">点击上方按钮创建您的第一个知识点</p>
      <button
        @click="openModal()"
        class="btn-primary inline-flex items-center gap-2"
      >
        <Plus class="w-5 h-5" />
        <span>新建知识点</span>
      </button>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="modal-backdrop z-50">
      <div class="modal-card max-w-lg">
        <div class="flex items-center justify-between pb-4 mb-4 border-b">
          <h3 class="text-lg font-semibold text-main">
            {{ isEditing ? '编辑知识点' : '新建知识点' }}
          </h3>
          <button
            @click="closeModal"
            class="icon-btn"
          >
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-muted mb-2">知识点名称 *</label>
            <input
              v-model="currentItem.name"
              type="text"
              placeholder="请输入知识点名称"
              class="input"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-muted mb-2">分类路径</label>
            <input
              v-model="currentItem.path"
              type="text"
              placeholder="请输入分类路径，如: 数学/代数"
              class="input"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-muted mb-2">知识点描述</label>
            <textarea
              v-model="currentItem.description"
              rows="4"
              placeholder="请输入知识点描述"
              class="input"
            ></textarea>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 pt-4 mt-4 border-t">
          <button
            @click="closeModal"
            class="btn-secondary"
          >
            取消
          </button>
          <button
            @click="saveKnowledgePoint"
            class="btn-primary flex items-center gap-2"
          >
            <Save class="w-4 h-4" />
            <span>{{ isEditing ? '保存修改' : '创建' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
