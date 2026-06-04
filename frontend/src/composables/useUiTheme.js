const STORAGE_KEY = 'jellystudy-ui-preferences'

export const UI_MODES = [
  { id: 'light', label: '浅色' },
  { id: 'dark', label: '深色' },
  { id: 'system', label: '跟随系统' }
]

export const UI_ACCENTS = [
  { id: 'ocean', label: '海洋蓝', swatch: '#3b82f6' },
  { id: 'violet', label: '紫罗兰', swatch: '#8b5cf6' },
  { id: 'emerald', label: '翡翠绿', swatch: '#10b981' },
  { id: 'sunset', label: '暮光橙', swatch: '#f97316' }
]

const MODE_IDS = UI_MODES.map((m) => m.id)
const ACCENT_IDS = UI_ACCENTS.map((a) => a.id)

const DEFAULT_PREFS = { mode: 'light', accent: 'ocean' }

let systemListener = null

function resolveTheme(mode) {
  if (mode === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return mode === 'dark' ? 'dark' : 'light'
}

export function getStoredPreferences() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_PREFS }
    const legacy = raw === 'light' || raw === 'dark'
    if (legacy) {
      return { mode: raw, accent: 'ocean' }
    }
    const p = JSON.parse(raw)
    return {
      mode: MODE_IDS.includes(p.mode) ? p.mode : DEFAULT_PREFS.mode,
      accent: ACCENT_IDS.includes(p.accent) ? p.accent : DEFAULT_PREFS.accent
    }
  } catch {
    return { ...DEFAULT_PREFS }
  }
}

function persistPreferences(prefs) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs))
  } catch {
    /* ignore */
  }
}

export function applyUiPreferences(prefs) {
  const mode = MODE_IDS.includes(prefs.mode) ? prefs.mode : DEFAULT_PREFS.mode
  const accent = ACCENT_IDS.includes(prefs.accent) ? prefs.accent : DEFAULT_PREFS.accent
  const resolved = resolveTheme(mode)

  document.documentElement.setAttribute('data-theme', resolved)
  document.documentElement.setAttribute('data-accent', accent)
  document.documentElement.setAttribute('data-mode-pref', mode)

  persistPreferences({ mode, accent })
  return { mode, accent, resolved }
}

export function setUiPreference(key, value, current) {
  const next = { ...current, [key]: value }
  applyUiPreferences(next)
  return next
}

/** @deprecated use applyUiPreferences */
export function getStoredTheme() {
  return getStoredPreferences().mode === 'dark' ? 'dark' : 'light'
}

/** @deprecated */
export function applyUiTheme(theme) {
  return applyUiPreferences({ mode: theme === 'dark' ? 'dark' : 'light', accent: 'ocean' }).resolved
}

/** @deprecated */
export function toggleUiTheme(current) {
  const resolved = current === 'dark' ? 'light' : 'dark'
  applyUiPreferences({ mode: resolved, accent: getStoredPreferences().accent })
  return resolved
}

export function watchSystemTheme(onChange) {
  if (systemListener) {
    window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemListener)
  }
  systemListener = () => {
    const prefs = getStoredPreferences()
    if (prefs.mode === 'system') onChange(applyUiPreferences(prefs))
  }
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemListener)
}

export function unwatchSystemTheme() {
  if (!systemListener) return
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemListener)
  systemListener = null
}
