import axios from 'axios'
import { getToken, clearAuth } from './userContext.js'
export { unwrapApiData, unwrapApiList } from '../utils/apiResponse.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const apiKey = import.meta.env.VITE_JELLYSTUDY_API_KEY
if (apiKey) {
  api.defaults.headers.common['X-API-Key'] = apiKey
}

api.interceptors.request.use((config) => {
  const t = getToken()
  if (t) {
    config.headers.Authorization = `Bearer ${t}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = String(error.config?.url || '')
      const needsAuth = ['/coach', '/questions', '/answers', '/knowledge-points', '/auth/me'].some(
        (p) => url.includes(p)
      )
      if (needsAuth) {
        clearAuth()
        window.dispatchEvent(new CustomEvent('jellystudy:auth-required'))
      }
    }
    return Promise.reject(error)
  }
)

// 认证 API
export const authAPI = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (username, password, displayName) =>
    api.post('/auth/register', { username, password, displayName }),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
  hint: () => api.get('/auth/hint')
}

// 知识点API
export const knowledgePointAPI = {
  getAll: () => api.get('/knowledge-points'),
  getById: (id) => api.get(`/knowledge-points/${id}`),
  create: (data) => api.post('/knowledge-points', data),
  update: (id, data) => api.put(`/knowledge-points/${id}`, data),
  delete: (id) => api.delete(`/knowledge-points/${id}`)
}

// 问题API
export const questionAPI = {
  getAll: () => api.get('/questions'),
  getById: (id) => api.get(`/questions/${id}`),
  create: (data) => api.post('/questions', data),
  update: (id, data) => api.put(`/questions/${id}`, data),
  delete: (id) => api.delete(`/questions/${id}`),
  getHot: () => api.get('/questions/hot'),
  getRecommended: () => api.get('/questions/recommended'),
  search: (keyword) => api.get('/questions/search', { params: { keyword } })
}

// 回答API
export const answerAPI = {
  getAll: () => api.get('/answers'),
  getById: (id) => api.get(`/answers/${id}`),
  create: (data) => api.post('/answers', data),
  update: (id, data) => api.put(`/answers/${id}`, data),
  delete: (id) => api.delete(`/answers/${id}`),
  getByQuestion: (questionId) => api.get(`/answers/question/${questionId}`),
  getHighLiked: (questionId) => api.get(`/answers/question/${questionId}/high-liked`),
  like: (id) => api.post(`/answers/${id}/like`),
  comment: (id, data) => api.post(`/answers/${id}/comments`, data)
}

// 评估API
export const evaluationAPI = {
  getAllQuestionEvaluations: () => api.get('/evaluations/questions'),
  getAllAnswerEvaluations: () => api.get('/evaluations/answers'),
  getQuestionEvaluation: (questionId) => api.get(`/evaluations/questions/${questionId}`),
  getAnswerEvaluation: (answerId) => api.get(`/evaluations/answers/${answerId}`),
  evaluateQuestion: (questionId, questionTitle, questionContent) =>
    api.post('/evaluations/question', { questionId, questionTitle, questionContent }),
  evaluateAnswer: (answerId, questionId, questionContent, answerContent, userId) =>
    api.post('/evaluations/answer', {
      answerId,
      questionId,
      questionContent,
      answerContent,
      userId
    }),
  getQuestionEvaluationsByDifficulty: (difficulty) =>
    api.get(`/evaluations/questions/difficulty/${difficulty}`),
  getAnswerEvaluationsByGrade: (grade) =>
    api.get(`/evaluations/answers/grade/${grade}`),
  deleteByQuestionId: (questionId) =>
    api.delete(`/evaluations/questions/by-question/${questionId}`),
  getInstanceInfo: () => api.get('/evaluations/instance-info')
}

// JellyCoach（userId 由 Bearer Token 解析，不可伪造）
export const coachAPI = {
  getProfile: () => api.get('/coach/profile'),
  getTodayTasks: () => api.get('/coach/tasks/today'),
  getPet: () => api.get('/coach/pet'),
  feedPet: (points = 10) => api.post('/coach/pet/feed', null, { params: { points } }),
  switchPetTheme: (theme) => api.post('/coach/pet/theme', null, { params: { theme } }),
  getWeeklyReport: () => api.get('/coach/report/weekly'),
  getPointsTrend: () => api.get('/coach/report/trend'),
  generateQuiz: (weakPoint) => api.post('/coach/quiz/generate', null, { params: { weakPoint } }),
  submitQuiz: (quizId, answer) => api.post(`/coach/quiz/${quizId}/submit`, { answer }),
  syncKnowledge: () => api.post('/coach/sync-knowledge'),
  getLeaderboard: (limit = 10) => api.get('/coach/leaderboard', { params: { limit } }),
  socraticAsk: (topic, message, history = [], teachMode = 'socratic') =>
    api.post('/coach/socratic', { message, history, teachMode }, { params: { topic } }),
  socraticSummary: (topic, history) =>
    api.post('/coach/socratic/summary', { message: '', history }, { params: { topic } }),
  getConfig: () => api.get('/coach/config')
}

export default api
