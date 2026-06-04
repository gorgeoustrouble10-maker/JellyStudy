<script setup>
import { Sun, Moon, Monitor } from 'lucide-vue-next'
import { UI_MODES, UI_ACCENTS } from '../composables/useUiTheme.js'

const props = defineProps({
  mode: { type: String, required: true },
  accent: { type: String, required: true },
  resolved: { type: String, default: 'light' }
})

const emit = defineEmits(['update:mode', 'update:accent'])

const modeIcons = { light: Sun, dark: Moon, system: Monitor }

const pickMode = (id) => emit('update:mode', id)
const pickAccent = (id) => emit('update:accent', id)
</script>

<template>
  <section class="theme-panel" aria-label="外观设置">
    <p class="theme-panel-title">外观</p>

    <div class="theme-mode-row" role="group" aria-label="明暗模式">
      <button
        v-for="m in UI_MODES"
        :key="m.id"
        type="button"
        class="theme-mode-btn"
        :class="{ 'theme-mode-btn-active': mode === m.id }"
        :title="m.label"
        @click="pickMode(m.id)"
      >
        <component :is="modeIcons[m.id]" class="w-3.5 h-3.5 shrink-0" />
        <span>{{ m.label }}</span>
      </button>
    </div>

    <p v-if="mode === 'system'" class="theme-hint">
      当前为{{ resolved === 'dark' ? '深色' : '浅色' }}（跟随系统）
    </p>

    <p class="theme-panel-sub">主题色</p>
    <div class="theme-accent-row" role="group" aria-label="主题色">
      <button
        v-for="a in UI_ACCENTS"
        :key="a.id"
        type="button"
        class="theme-accent-btn"
        :class="{ 'theme-accent-btn-active': accent === a.id }"
        :title="a.label"
        :style="{ '--swatch': a.swatch }"
        @click="pickAccent(a.id)"
      >
        <span class="theme-accent-swatch" />
        <span class="absolute w-px h-px overflow-hidden">{{ a.label }}</span>
      </button>
    </div>
    <p class="theme-accent-label">{{ UI_ACCENTS.find((a) => a.id === accent)?.label }}</p>
  </section>
</template>

<style scoped>
.theme-panel {
  @apply mt-6 p-3 rounded-xl border;
  background-color: rgb(var(--surface-muted) / 0.65);
  border-color: rgb(var(--border));
}

.theme-panel-title {
  @apply text-xs font-semibold uppercase tracking-wide mb-2;
  color: rgb(var(--text-muted));
}

.theme-panel-sub {
  @apply text-[11px] mt-3 mb-2;
  color: rgb(var(--text-muted));
}

.theme-mode-row {
  @apply grid grid-cols-3 gap-1;
}

.theme-mode-btn {
  @apply flex flex-col items-center gap-0.5 py-2 px-1 rounded-lg text-[10px] font-medium
         transition-all duration-200 active:scale-[0.97];
  color: rgb(var(--text-muted));
}

.theme-mode-btn:hover {
  background-color: rgb(var(--surface-card));
  color: rgb(var(--text));
}

.theme-mode-btn-active {
  background-color: rgb(var(--surface-card));
  color: rgb(var(--primary-700));
  box-shadow:
    0 1px 3px rgb(0 0 0 / 0.08),
    inset 0 0 0 1px rgb(var(--primary-200));
}

[data-theme='dark'] .theme-mode-btn-active {
  color: rgb(var(--primary-300));
  box-shadow:
    0 1px 3px rgb(0 0 0 / 0.2),
    inset 0 0 0 1px rgb(var(--primary-700));
}

.theme-hint {
  @apply text-[10px] mt-1.5 text-center;
  color: rgb(var(--text-muted));
}

.theme-accent-row {
  @apply flex justify-center gap-2;
}

.theme-accent-btn {
  @apply w-9 h-9 rounded-full flex items-center justify-center
         transition-all duration-200 active:scale-90;
  border: 2px solid transparent;
}

.theme-accent-swatch {
  @apply w-6 h-6 rounded-full shadow-sm;
  background: var(--swatch);
}

.theme-accent-btn-active {
  border-color: rgb(var(--primary-500));
  transform: scale(1.08);
}

.theme-accent-label {
  @apply text-[10px] text-center mt-1.5;
  color: rgb(var(--text-muted));
}
</style>
