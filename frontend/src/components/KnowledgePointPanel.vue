<script setup>import { ref, onMounted } from 'vue';
import { Plus, Search, Edit2, Trash2, Eye, Save, X, BookOpen, Clock } from 'lucide-vue-next';
import { knowledgePointAPI, questionAPI } from '../services/api';
import { requireLogin } from '../utils/requireLogin.js';
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
 const points = kpRes.data.data || [];
 const questions = qRes.data.data || [];
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
        <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索知识点..."
          class="w-full sm:w-64 pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
        />
      </div>
      <button
        @click="openModal()"
        class="flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all duration-200 shadow-md hover:shadow-lg"
      >
        <Plus class="w-5 h-5" />
        <span>新建知识点</span>
      </button>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700">
      {{ error }}
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- 知识点列表 -->
    <div v-else-if="filteredPoints().length > 0" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="point in filteredPoints()"
        :key="point.id"
        class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-lg transition-all duration-300 hover:-translate-y-1 group"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-2">
            <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
              <BookOpen class="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <h3 class="font-semibold text-gray-900 group-hover:text-primary-600 transition-colors">
                {{ point.name }}
              </h3>
              <span v-if="point.path" class="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                {{ point.path }}
              </span>
            </div>
          </div>
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              @click="openModal(point)"
              class="p-2 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
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
        
        <p class="text-gray-600 text-sm mb-4 line-clamp-2">{{ point.description }}</p>
        
        <div class="flex items-center justify-between text-xs text-gray-400">
          <div class="flex items-center gap-1">
            <Clock class="w-3 h-3" />
            <span>{{ formatDate(point.createdAt) }}</span>
          </div>
          <span>{{ point.questionCount || 0 }} 个问题</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-16">
      <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <BookOpen class="w-10 h-10 text-gray-400" />
      </div>
      <h3 class="text-lg font-medium text-gray-900 mb-2">暂无知识点</h3>
      <p class="text-gray-500 mb-6">点击上方按钮创建您的第一个知识点</p>
      <button
        @click="openModal()"
        class="inline-flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all"
      >
        <Plus class="w-5 h-5" />
        <span>新建知识点</span>
      </button>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg animate-fadeIn">
        <div class="flex items-center justify-between p-6 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ isEditing ? '编辑知识点' : '新建知识点' }}
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
            <label class="block text-sm font-medium text-gray-700 mb-2">知识点名称 *</label>
            <input
              v-model="currentItem.name"
              type="text"
              placeholder="请输入知识点名称"
              class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">分类路径</label>
            <input
              v-model="currentItem.path"
              type="text"
              placeholder="请输入分类路径，如: 数学/代数"
              class="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">知识点描述</label>
            <textarea
              v-model="currentItem.description"
              rows="4"
              placeholder="请输入知识点描述"
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
            @click="saveKnowledgePoint"
            class="flex items-center gap-2 px-5 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-xl transition-all"
          >
            <Save class="w-4 h-4" />
            <span>{{ isEditing ? '保存修改' : '创建' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
