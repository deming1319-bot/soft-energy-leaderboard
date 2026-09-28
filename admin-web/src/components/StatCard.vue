<script setup lang="ts">
import type { Component } from 'vue'
defineProps<{ label: string; value: number | string; note: string; icon: Component; tone?: string }>()
</script>

<template>
  <article class="stat-card surface" :class="tone ? `stat-card--${tone}` : ''">
    <div class="stat-card__head"><span>{{ label }}</span><div class="stat-card__icon"><component :is="icon" /></div></div>
    <strong>{{ value }}</strong>
    <p>{{ note }}</p>
  </article>
</template>

<style scoped>
.stat-card { position: relative; padding: 22px 23px 20px; overflow: hidden; transition: transform .2s ease, box-shadow .2s ease; }
.stat-card::after { content: ''; position: absolute; right: -24px; bottom: -42px; width: 100px; height: 100px; border: 1px solid rgba(137,59,49,.1); border-radius: 50%; }
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-soft); }
.stat-card__head { display: flex; align-items: center; justify-content: space-between; color: var(--ink-500); font-size: 13px; }
.stat-card__icon { width: 37px; height: 37px; display: grid; place-items: center; border-radius: 11px; color: var(--cinnabar-600); background: #f7ebe8; font-size: 18px; }
.stat-card strong { display: block; margin-top: 18px; font-family: var(--font-serif); font-size: 34px; font-weight: 650; line-height: 1; }
.stat-card p { margin: 10px 0 0; color: var(--ink-500); font-size: 12px; }
.stat-card--pine .stat-card__icon { color: var(--pine-700); background: var(--pine-100); }
.stat-card--gold .stat-card__icon { color: var(--gold-600); background: var(--gold-100); }
</style>
