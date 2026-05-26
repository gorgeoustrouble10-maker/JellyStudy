import { ref, computed } from 'vue'

const TOKEN_KEY = 'jellystudy_auth_token'
const USER_KEY = 'jellystudy_user_id'
const NAME_KEY = 'jellystudy_display_name'

export const token = ref(localStorage.getItem(TOKEN_KEY) || '')
export const userId = ref(localStorage.getItem(USER_KEY) || '')
export const displayName = ref(localStorage.getItem(NAME_KEY) || '')

export const isLoggedIn = computed(() => !!token.value && !!userId.value)

export function getToken() {
  return token.value
}

export function getUserId() {
  return userId.value
}

export function applyAuthSession(session) {
  if (!session?.token || !session?.userId) return
  token.value = session.token
  userId.value = session.userId
  displayName.value = session.displayName || session.userId
  localStorage.setItem(TOKEN_KEY, token.value)
  localStorage.setItem(USER_KEY, userId.value)
  localStorage.setItem(NAME_KEY, displayName.value)
}

export function clearAuth() {
  token.value = ''
  userId.value = ''
  displayName.value = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(NAME_KEY)
}

/** @deprecated 保留兼容；请使用 applyAuthSession */
export function setUserId(id) {
  userId.value = (id && String(id).trim()) || ''
  localStorage.setItem(USER_KEY, userId.value)
}
