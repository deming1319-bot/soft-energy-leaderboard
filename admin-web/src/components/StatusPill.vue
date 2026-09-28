<script setup lang="ts">
import { computed } from 'vue'
import type { ActivityStatus, SubmissionGrade, TimeState } from '@/types'

const props = defineProps<{ value: ActivityStatus | SubmissionGrade | TimeState | string }>()
const labels: Record<string, string> = {
  DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已截止', ARCHIVED: '已归档',
  UPCOMING: '未开始', ACTIVE: '进行中', ENDED: '已结束',
  CHAMPION: '冠军', RUNNER_UP: '亚军', REVIEW_REQUIRED: '待复核', UNRANKED: '未入榜',
  ENABLED: '正常', DISABLED: '已停用',
  PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回',
}
const tone = computed(() => ({
  CHAMPION: 'champion', ACTIVE: 'active', PUBLISHED: 'active',
  RUNNER_UP: 'runner', REVIEW_REQUIRED: 'review', UPCOMING: 'upcoming',
  CLOSED: 'neutral', ENDED: 'neutral', ARCHIVED: 'neutral', DISABLED: 'neutral',
  PENDING: 'review', PROCESSING: 'upcoming', RESOLVED: 'active', REJECTED: 'neutral',
}[props.value] || 'neutral'))
</script>

<template><span class="pill" :class="`pill--${tone}`"><i />{{ labels[value] || value }}</span></template>

<style scoped>
.pill { display: inline-flex; align-items: center; gap: 7px; min-height: 27px; padding: 4px 10px; border-radius: 999px; background: #f0eee8; color: var(--ink-650); font-size: 12px; font-weight: 650; white-space: nowrap; }
.pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; opacity: .7; }
.pill--active { color: var(--pine-700); background: var(--pine-100); }
.pill--champion { color: #8a6432; background: var(--gold-100); }
.pill--runner { color: #6e5b75; background: #eee8f0; }
.pill--review { color: var(--cinnabar-700); background: #f5e6e3; }
.pill--upcoming { color: #466376; background: #e5edf1; }
</style>
