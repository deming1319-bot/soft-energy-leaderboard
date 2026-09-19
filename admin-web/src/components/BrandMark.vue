<script setup lang="ts">
withDefaults(defineProps<{ compact?: boolean; light?: boolean }>(), { compact: false, light: false })
</script>

<template>
  <div class="brand" :class="{ compact, light }">
    <div class="brand__seal" aria-hidden="true"><span>和</span></div>
    <div v-if="!compact" class="brand__copy">
      <strong>问道</strong>
      <small>修习答题管理台</small>
    </div>
  </div>
</template>

<style scoped>
.brand { display: flex; align-items: center; gap: 13px; color: var(--ink-950); }
.brand__seal { width: 43px; height: 43px; display: grid; place-items: center; border-radius: 13px 13px 13px 5px; color: #fffaf2; background: var(--cinnabar-600); box-shadow: 0 8px 22px rgba(137, 59, 49, .2); }
.brand__seal span { font-family: var(--font-serif); font-size: 24px; line-height: 1; transform: translateY(-1px); }
.brand__copy { display: grid; gap: 2px; }
.brand__copy strong { font-family: var(--font-serif); font-size: 21px; font-weight: 700; letter-spacing: .16em; }
.brand__copy small { color: var(--ink-500); font-size: 10px; letter-spacing: .12em; }
.brand.light { color: #fff; }
.brand.light .brand__copy small { color: rgba(255,255,255,.58); }
.brand.compact .brand__seal { width: 38px; height: 38px; }
</style>
