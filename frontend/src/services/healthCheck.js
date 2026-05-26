import axios from 'axios'

const client = axios.create({ timeout: 4000 })

const SERVICE_CHECKS = [
  { key: 'knowledge', url: '/api/health/knowledge', label: '知识点服务', port: '8081' },
  { key: 'qa', url: '/api/health/qa', label: '问答服务', port: '8082' },
  { key: 'evaluate', url: '/api/health/evaluate', label: '评估服务', port: '8083' },
  { key: 'coach', url: '/api/health/coach', label: '成长教练', port: '8084' }
]

function isUpResponse(res) {
  if (res.status !== 200) return false
  const data = res.data?.data ?? res.data
  return data?.status === 'UP' || data?.status === 'DEGRADED'
}

export async function checkAllServices() {
  const results = await Promise.all(
    SERVICE_CHECKS.map(async (svc) => {
      try {
        const res = await client.get(svc.url)
        const data = res.data?.data ?? res.data
        return {
          ...svc,
          up: isUpResponse(res),
          service: data?.service ?? null,
          components: data?.components ?? null,
          displayLabel: data?.service ? `${svc.label} · ${svc.port}` : `${svc.label} · ${svc.port}`
        }
      } catch {
        return { ...svc, up: false, service: null }
      }
    })
  )

  const upCount = results.filter((r) => r.up).length
  let status = 'offline'
  let label = '未连接'
  if (upCount === SERVICE_CHECKS.length) {
    status = 'connected'
    label = '已连接'
  } else if (upCount > 0) {
    status = 'partial'
    label = `部分可用 (${upCount}/${SERVICE_CHECKS.length})`
  }

  return { status, label, results }
}
