/**
 * 统一解析后端 API 错误信息
 */
export function extractApiError(err, fallback = '未知错误') {
  const d = err?.response?.data
  if (d?.message) return d.message
  if (typeof d === 'string') return d
  const status = err?.response?.status
  if (status === 502 || status === 503) {
    return '后端服务未就绪，请等待 30 秒后重试'
  }
  if (status === 500) {
    return '服务暂不可用（可能仍在启动），请稍后重试'
  }
  if (status === 401) {
    return '请先登录'
  }
  return err?.message || fallback
}
