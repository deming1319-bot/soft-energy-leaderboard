<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Clock, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { auditApi } from '@/api/services'

interface AuditItem { id?: string; adminName?: string; action?: string; targetType?: string; targetId?: string; detail?: string; ipAddress?: string; createdAt?: string }
const loading = ref(true)
const keyword = ref('')
const rows = ref<AuditItem[]>([])
const labels: Record<string,string> = { LOGIN: '管理员登录', CREATE_ACTIVITY: '创建题目', UPDATE_ACTIVITY: '编辑题目', PUBLISH_ACTIVITY: '发布题目', CLOSE_ACTIVITY: '截止题目', REVIEW_SUBMISSION: '人工复核答题' }
const filtered = computed(() => rows.value.filter(x => JSON.stringify(x).toLowerCase().includes(keyword.value.toLowerCase())))
onMounted(async () => { try { rows.value = (await auditApi.list()) as AuditItem[] } finally { loading.value = false } })
</script>

<template>
  <div class="page-shell">
    <div class="page-heading"><div><span class="eyebrow">AUDIT TRAIL</span><h1>操作记录</h1><p>保留关键管理动作，便于问题追踪和交接审计。</p></div></div>
    <section class="audit-intro"><Clock/><div><b>安全审计已开启</b><span>登录、题目变更、发布截止及人工复核均会留痕，业务数据不会在此页面修改。</span></div></section>
    <section class="surface audit-panel">
      <header><div><h3>最近操作</h3><span>共 {{ filtered.length }} 条记录</span></div><el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索操作或对象" /></header>
      <div v-loading="loading" class="timeline">
        <article v-for="(item,index) in filtered" :key="item.id || index">
          <div class="timeline__date"><b>{{ item.createdAt ? dayjs(item.createdAt).format('MM-DD') : '--' }}</b><span>{{ item.createdAt ? dayjs(item.createdAt).format('HH:mm:ss') : '--' }}</span></div>
          <div class="timeline__mark"><i /></div>
          <div class="timeline__content"><div><b>{{ labels[item.action || ''] || item.action || '系统操作' }}</b><span>{{ item.adminName || '管理员' }}</span></div><p>{{ item.detail || `${item.targetType || '对象'} ${item.targetId || ''}` }}</p><small v-if="item.ipAddress">IP {{ item.ipAddress }}</small></div>
        </article>
        <el-empty v-if="!filtered.length" description="暂无操作记录" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.audit-intro{margin-bottom:20px;padding:17px 20px;display:flex;align-items:center;gap:14px;color:var(--pine-700);background:var(--pine-100);border-radius:14px}.audit-intro svg{width:25px}.audit-intro div{display:grid;gap:4px}.audit-intro b{font-size:13px}.audit-intro span{color:var(--ink-500);font-size:11px}.audit-panel{padding:22px 26px}.audit-panel>header{display:flex;align-items:center;justify-content:space-between;padding-bottom:20px;border-bottom:1px solid var(--paper-200)}.audit-panel h3{margin:0;font-family:var(--font-serif);font-size:20px}.audit-panel header span{color:var(--ink-500);font-size:11px}.audit-panel .el-input{width:260px}.timeline{padding:20px 0;min-height:250px}.timeline article{display:grid;grid-template-columns:66px 26px 1fr;min-height:100px}.timeline__date{padding-top:3px;display:grid;align-content:start;text-align:right}.timeline__date b{font-family:var(--font-serif);font-size:14px}.timeline__date span{color:var(--ink-500);font-size:10px}.timeline__mark{position:relative}.timeline__mark::after{content:'';position:absolute;top:16px;bottom:-3px;left:50%;width:1px;background:var(--paper-300)}.timeline article:last-child .timeline__mark::after{display:none}.timeline__mark i{position:relative;z-index:1;margin:7px auto;display:block;width:10px;height:10px;border:2px solid #fff;border-radius:50%;background:var(--cinnabar-500);box-shadow:0 0 0 2px #e3c1bb}.timeline__content{margin-left:13px;padding:0 0 22px 3px}.timeline__content>div{display:flex;align-items:center;gap:10px}.timeline__content>div>b{font-size:14px}.timeline__content>div>span{padding:3px 7px;border-radius:6px;color:var(--ink-500);background:var(--paper-100);font-size:10px}.timeline__content p{margin:7px 0;color:var(--ink-650);font-size:12px;line-height:1.6}.timeline__content small{color:var(--ink-500);font-size:10px}
@media(max-width:600px){.audit-panel{padding:18px 14px}.audit-panel>header{align-items:flex-start;flex-direction:column;gap:13px}.audit-panel .el-input{width:100%}.timeline article{grid-template-columns:54px 22px 1fr}.timeline__content{margin-left:7px}}
</style>
