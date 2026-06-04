<script setup>
defineProps({
  label: { type: String, default: '加载中…' },
  size: { type: String, default: 'md' }
})
</script>

<template>
  <div class="page-loading" role="status" aria-live="polite">
    <div
      class="page-loading-spinner"
      :class="size === 'sm' ? 'w-6 h-6 border-2' : 'w-9 h-9 border-[3px]'"
    />
    <p v-if="label" class="page-loading-label">{{ label }}</p>
  </div>
</template>
