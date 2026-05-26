import { isLoggedIn } from '../services/userContext.js'

/** 写操作前检查登录；未登录时弹出登录框 */
export function requireLogin() {
  if (isLoggedIn.value) {
    return true
  }
  window.dispatchEvent(new CustomEvent('jellystudy:auth-required'))
  return false
}
