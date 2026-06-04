/**
 * 统一解析后端 ApiResponse 信封或直接 DTO/数组。
 * 约定：code===200 且存在 data 字段时取 data；否则退回整包 body。
 */
export function unwrapApiData(response) {
  const body = response?.data
  if (body == null) return body
  if (typeof body === 'object' && !Array.isArray(body) && 'data' in body) {
    const code = body.code
    if (code === undefined || code === 200) {
      return body.data !== undefined ? body.data : body
    }
  }
  return body
}

export function unwrapApiList(response) {
  const data = unwrapApiData(response)
  return Array.isArray(data) ? data : []
}
