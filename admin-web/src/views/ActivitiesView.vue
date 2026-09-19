<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { activityApi } from '@/api/services'
import StatusPill from '@/components/StatusPill.vue'
import type { ActivitySummary } from '@/types'

const router = useRouter()
const loading = ref(true)
const keyword = ref('')
const status = ref('ALL')
const rows = ref<ActivitySummary[]>([])
const categoryLabel: Record<string, string> = { CLASSICS: '经典', PHILOSOPHY: '哲思', LIFE_PRACTICE: '实践', CONFUCIAN: '传统', BUDDHIST: '待确认', TAOIST: '待确认', INTEGRATED: '待确认', MASTER_ORIGINAL: '待确认' }
const filtered = computed(() => rows.value.filter((item) => (status.value === 'ALL' || item.timeState === status.value || item.status === status.value) && item.title.toLowerCase().includes(keyword.value.trim().toLowerCase())))

async function load() { loading.value = true; try { rows.value = await activityApi.list() } finally { loading.value = false } }
async function publish(item: ActivitySummary) { await ElMessageBox.confirm(`发布“${item.title}”后，学员将在设定时间看到题目。`, '确认发布', { confirmButtonText: '确认发布', cancelButtonText: '暂不发布' }); await activityApi.publish(item.id); ElMessage.success('题目已发布'); await load() }
async function close(item: ActivitySummary) { await ElMessageBox.confirm(`将立即停止“${item.title}”的答题并生成榜单。`, '提前截止', { type: 'warning', confirmButtonText: '确认截止', cancelButtonText: '取消' }); await activityApi.close(item.id); ElMessage.success('题目已截止'); await load() }
async function removeDraft(item: ActivitySummary) {
  const confirmed = await ElMessageBox.confirm(
    `确定删除草稿“${item.title}”吗？删除后无法恢复。`,
    '删除草稿题目',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  await activityApi.remove(item.id)
  ElMessage.success('草稿题目已删除')
  await load()
}
onMounted(load)
</script>

<template>
  <div class="page-shell">
    <div class="page-heading">
      <div><span class="eyebrow">QUESTIONS</span><h1>题目与榜单</h1><p>创建修习题目、控制开放时间，并查看每位学员的作答结果。</p></div>
      <div class="page-actions"><el-button type="primary" :icon="Plus" @click="router.push('/activities/new')">创建新题</el-button></div>
    </div>

    <section class="surface activity-panel">
      <div class="toolbar">
        <div class="filters">
          <button v-for="item in [{v:'ALL',l:'全部'},{v:'ACTIVE',l:'进行中'},{v:'UPCOMING',l:'未开始'},{v:'ENDED',l:'已结束'},{v:'DRAFT',l:'草稿'}]" :key="item.v" :class="{ active: status === item.v }" @click="status = item.v">{{ item.l }}</button>
        </div>
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索题目名称" class="search" />
      </div>

      <div v-loading="loading" class="table-wrap" :class="{ 'is-empty': !loading && !filtered.length }">
        <div class="desktop-activity-table">
          <el-table :data="filtered" style="width:100%" @row-click="(row: ActivitySummary) => router.push(`/activities/${row.id}`)">
            <el-table-column label="题目信息" min-width="280">
              <template #default="scope"><div class="title-cell"><span class="category">{{ categoryLabel[scope.row.category] }}</span><div><b>{{ scope.row.title }}</b><small>{{ scope.row.subtitle || '未设置副标题' }}</small></div></div></template>
            </el-table-column>
            <el-table-column label="状态" width="105"><template #default="scope"><StatusPill :value="scope.row.timeState" /></template></el-table-column>
            <el-table-column label="开放时间" min-width="190"><template #default="scope"><div class="time-cell"><span>{{ dayjs(scope.row.startAt).format('MM-DD HH:mm') }}</span><i>至</i><span>{{ dayjs(scope.row.endAt).format('MM-DD HH:mm') }}</span></div></template></el-table-column>
            <el-table-column label="答题 / 入榜" width="140"><template #default="scope"><b>{{ scope.row.submissionCount }}</b><span class="muted"> / {{ scope.row.championCount + scope.row.runnerUpCount }}</span></template></el-table-column>
            <el-table-column label="操作" width="210" fixed="right"><template #default="scope"><div class="row-actions" @click.stop><button type="button" class="row-action" @click="router.push(`/activities/${scope.row.id}`)">详情</button><button v-if="scope.row.status === 'DRAFT'" type="button" class="row-action" @click="router.push(`/activities/${scope.row.id}/edit`)">编辑</button><button v-if="scope.row.status === 'DRAFT'" type="button" class="row-action row-action--primary" @click="publish(scope.row)">发布</button><button v-if="scope.row.status === 'DRAFT'" type="button" class="row-action row-action--danger" @click="removeDraft(scope.row)">删除</button><button v-if="scope.row.timeState === 'ACTIVE'" type="button" class="row-action row-action--danger" @click="close(scope.row)">截止</button></div></template></el-table-column>
            <template #empty><el-empty description="没有符合条件的题目"><el-button type="primary" @click="router.push('/activities/new')">创建第一道题</el-button></el-empty></template>
          </el-table>
        </div>

        <div class="mobile-activity-list">
          <article v-for="item in filtered" :key="item.id" class="activity-card" @click="router.push(`/activities/${item.id}`)">
            <header>
              <div class="activity-card__identity">
                <span class="category">{{ categoryLabel[item.category] }}</span>
                <div><h3>{{ item.title }}</h3><p>{{ item.subtitle || '未设置副标题' }}</p></div>
              </div>
              <StatusPill :value="item.timeState" />
            </header>
            <dl>
              <div><dt>开放时间</dt><dd>{{ dayjs(item.startAt).format('MM-DD HH:mm') }}<i>至</i>{{ dayjs(item.endAt).format('MM-DD HH:mm') }}</dd></div>
              <div><dt>答题 / 入榜</dt><dd><b>{{ item.submissionCount }}</b><i>/</i><b>{{ item.championCount + item.runnerUpCount }}</b></dd></div>
            </dl>
            <footer @click.stop>
              <button type="button" class="row-action" @click="router.push(`/activities/${item.id}`)">查看详情</button>
              <button v-if="item.status === 'DRAFT'" type="button" class="row-action" @click="router.push(`/activities/${item.id}/edit`)">编辑</button>
              <button v-if="item.status === 'DRAFT'" type="button" class="row-action row-action--primary" @click="publish(item)">发布</button>
              <button v-if="item.status === 'DRAFT'" type="button" class="row-action row-action--danger" @click="removeDraft(item)">删除</button>
              <button v-if="item.timeState === 'ACTIVE'" type="button" class="row-action row-action--danger" @click="close(item)">截止</button>
            </footer>
          </article>
          <div v-if="!loading && !filtered.length" class="mobile-empty-state">
            <span>问</span><b>没有符合条件的题目</b><p>调整筛选条件，或创建第一道修习题目。</p>
            <el-button type="primary" @click="router.push('/activities/new')">创建第一道题</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.activity-panel { overflow: hidden; }
.toolbar { min-height: 76px; padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--paper-200); }
.filters { display: flex; gap: 4px; padding: 4px; border-radius: 11px; background: var(--paper-100); }
.filters button { min-width: 64px; height: 34px; padding: 0 12px; border: 0; border-radius: 8px; color: var(--ink-500); background: transparent; cursor: pointer; font-size: 12px; }
.filters button.active { color: var(--cinnabar-700); background: #fff; box-shadow: 0 3px 10px rgba(61,54,45,.09); font-weight: 700; }
.search { width: 260px; }
.table-wrap { padding: 8px 14px 18px; }
.table-wrap.is-empty { min-height: 320px; }
.title-cell { padding: 8px 0; display: flex; align-items: center; gap: 13px; }
.category { width: 46px; height: 46px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 13px 13px 13px 5px; color: var(--cinnabar-700); background: #f4e8e3; font-family: var(--font-serif); font-size: 12px; }
.title-cell > div { min-width: 0; display: grid; gap: 4px; }
.title-cell b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.title-cell small { overflow: hidden; color: var(--ink-500); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.time-cell { display: flex; gap: 6px; align-items: center; color: var(--ink-650); font-size: 12px; }
.time-cell i { color: var(--paper-300); font-style: normal; }
.row-actions { display: flex; align-items: center; gap: 4px; white-space: nowrap; }
.row-action { min-width: 42px; height: 32px; padding: 0 9px; color: var(--ink-650); border: 0; border-radius: 8px; background: transparent; cursor: pointer; font-size: 12px; font-weight: 700; transition: color .18s ease, background .18s ease, box-shadow .18s ease; }
.row-action:hover { color: var(--ink-950); background: var(--paper-100); }
.row-action:focus-visible { outline: 3px solid rgba(163,72,59,.16); outline-offset: 1px; }
.row-action--primary { color: var(--cinnabar-700); background: #f6eae6; }
.row-action--primary:hover { color: #fff; background: var(--cinnabar-600); box-shadow: 0 5px 12px rgba(163,72,59,.16); }
.row-action--danger { color: #9a4539; background: #f7e8e5; }
.row-action--danger:hover { color: #fff; background: #a3483b; }
.mobile-activity-list { display: none; }
@media (max-width: 760px) {
  .activity-panel { overflow: hidden; }
  .toolbar { min-height: 0; padding: 16px 14px; align-items: stretch; flex-direction: column; gap: 14px; }
  .filters { width: 100%; gap: 2px; }
  .filters button { min-width: 0; padding-inline: 6px; flex: 1; }
  .search { width: 100%; }
  .table-wrap { min-height: 0; padding: 12px 12px 14px; }
  .table-wrap.is-empty { min-height: 0; }
  .desktop-activity-table { display: none; }
  .mobile-activity-list { display: grid; gap: 12px; }
  .activity-card { overflow: hidden; border: 1px solid var(--paper-200); border-radius: 16px; background: #fff; box-shadow: 0 7px 20px rgba(48,43,36,.045); cursor: pointer; }
  .activity-card>header { padding: 16px; display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
  .activity-card__identity { min-width: 0; display: flex; align-items: center; gap: 11px; }
  .activity-card__identity .category { width: 42px; height: 42px; }
  .activity-card__identity>div { min-width: 0; }
  .activity-card h3 { margin: 0; overflow: hidden; font-family: var(--font-serif); font-size: 18px; text-overflow: ellipsis; white-space: nowrap; }
  .activity-card p { margin: 4px 0 0; overflow: hidden; color: var(--ink-500); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
  .activity-card dl { margin: 0 16px; padding: 13px 0; display: grid; grid-template-columns:minmax(0,1.55fr) minmax(90px,.45fr); gap: 12px; border-top: 1px solid var(--paper-200); }
  .activity-card dl>div { min-width: 0; display: grid; gap: 5px; }
  .activity-card dt { color: var(--ink-500); font-size: 10px; }
  .activity-card dd { margin: 0; display: flex; align-items: center; gap: 5px; color: var(--ink-800); font-size: 11px; white-space: nowrap; }
  .activity-card dd i { color: var(--paper-300); font-style: normal; }
  .activity-card dd b { font-family: var(--font-serif); font-size: 16px; }
  .activity-card footer { padding: 10px 12px; display: flex; gap: 5px; border-top: 1px solid var(--paper-200); background: #fcfbf8; }
  .activity-card footer .row-action { min-width: 0; height: 38px; padding-inline: 7px; flex: 1; }
  .mobile-empty-state { min-height: 250px; padding: 30px 20px; display: grid; place-items: center; align-content: center; text-align: center; }
  .mobile-empty-state>span { width: 54px; height: 54px; display: grid; place-items: center; color: var(--cinnabar-600); border: 1px solid #dfc7c1; border-radius: 50%; background: #f8eeeb; font-family: var(--font-serif); font-size: 23px; }
  .mobile-empty-state>b { margin-top: 14px; font-family: var(--font-serif); font-size: 18px; }
  .mobile-empty-state p { margin: 6px 0 16px; white-space: normal; }
}
@media (max-width: 360px) {
  .activity-card dl { grid-template-columns: 1fr; gap: 10px; }
  .activity-card dl>div { grid-template-columns: 72px 1fr; align-items: center; gap: 8px; }
  .activity-card footer { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); }
}
</style>
