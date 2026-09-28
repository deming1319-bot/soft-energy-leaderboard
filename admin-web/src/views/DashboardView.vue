<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Medal, Tickets, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { dashboardApi } from '@/api/services'
import StatCard from '@/components/StatCard.vue'
import StatusPill from '@/components/StatusPill.vue'
import type { DashboardOverview } from '@/types'

const router = useRouter()
const loading = ref(true)
const data = ref<DashboardOverview>({ totalUsers: 0, activeActivities: 0, totalSubmissions: 0, championCount: 0, runnerUpCount: 0, reviewRequiredCount: 0, recentSubmissions: [] })
onMounted(async () => { try { data.value = await dashboardApi.overview() } finally { loading.value = false } })
const formatTime = (value: string) => dayjs(value).format('MM月DD日 HH:mm')
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <div class="page-heading">
      <div><span class="eyebrow">OVERVIEW</span><h1>今日修习总览</h1><p>从发布题目到榜单揭晓，关键进展一目了然。</p></div>
      <div class="page-actions"><el-button @click="router.push('/activities')">查看全部题目</el-button><el-button type="primary" @click="router.push('/activities/new')">发布新题</el-button></div>
    </div>

    <section class="stats-grid">
      <StatCard label="注册学员" :value="data.totalUsers" note="真实与测试学员总数" :icon="User" tone="pine" />
      <StatCard label="进行中题目" :value="data.activeActivities" note="当前开放作答的修习" :icon="Collection" />
      <StatCard label="累计答题" :value="data.totalSubmissions" note="包含有效与待复核提交" :icon="Tickets" tone="pine" />
      <StatCard label="冠军人次" :value="data.championCount" :note="`另有 ${data.runnerUpCount} 人次进入亚军`" :icon="Medal" tone="gold" />
    </section>

    <section class="dashboard-grid">
      <article class="surface recent-panel">
        <header><div><span class="eyebrow">LIVE ACTIVITY</span><h3>最新答题动态</h3></div><button type="button" @click="router.push('/activities')">查看题目 →</button></header>
        <el-table v-if="data.recentSubmissions.length" :data="data.recentSubmissions" style="width:100%">
          <el-table-column label="学员" min-width="130"><template #default="scope"><div class="learner"><span>{{ scope.row.userDisplayName.slice(0,1) }}</span><b>{{ scope.row.userDisplayName }}</b></div></template></el-table-column>
          <el-table-column prop="activityTitle" label="题目" min-width="220" show-overflow-tooltip />
          <el-table-column label="结果" width="100"><template #default="scope"><StatusPill :value="scope.row.grade" /></template></el-table-column>
          <el-table-column label="提交时间" width="150"><template #default="scope"><span class="muted">{{ formatTime(scope.row.submittedAt) }}</span></template></el-table-column>
        </el-table>
        <el-empty v-else description="暂无答题记录" />
      </article>

      <aside class="side-stack">
        <article class="surface review-card">
          <span class="review-card__number">{{ data.reviewRequiredCount }}</span>
          <div><span class="eyebrow">NEEDS REVIEW</span><h3>待人工复核</h3><p>语义接近但系统无法确定的回答，请管理员判断是否入榜。</p></div>
          <el-button plain @click="router.push('/activities')">前往处理</el-button>
        </article>
        <article class="wisdom-card">
          <span>今日一语</span>
          <blockquote>“致知在格物，物格而后知至。”</blockquote>
          <small>— 《礼记·大学》</small>
          <i>格</i>
        </article>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 17px; }
.dashboard-grid { margin-top: 20px; display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(280px, .7fr); gap: 20px; }
.recent-panel { padding: 24px; overflow: hidden; }
.recent-panel header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; }
h3 { margin: 6px 0 0; font-family: var(--font-serif); font-size: 21px; letter-spacing: .05em; }
.recent-panel header button { border: 0; color: var(--cinnabar-600); background: transparent; cursor: pointer; font-size: 12px; }
.learner { display: flex; align-items: center; gap: 10px; }
.learner span { width: 31px; height: 31px; display: grid; place-items: center; border-radius: 10px; color: #fff; background: var(--pine-600); font-family: var(--font-serif); }
.learner b { font-size: 13px; }
.side-stack { display: grid; gap: 20px; }
.review-card { padding: 25px; display: grid; grid-template-columns: auto 1fr; gap: 14px; }
.review-card__number { width: 54px; height: 54px; display: grid; place-items: center; border-radius: 16px; color: var(--cinnabar-700); background: #f4e5e2; font-family: var(--font-serif); font-size: 26px; }
.review-card p { margin: 9px 0 16px; color: var(--ink-500); font-size: 12px; line-height: 1.7; }
.review-card .el-button { grid-column: 1 / -1; }
.wisdom-card { position: relative; min-height: 225px; padding: 28px; overflow: hidden; color: #fff; border-radius: var(--radius-lg); background: var(--pine-700); }
.wisdom-card > span { color: #c9bb94; font-size: 11px; letter-spacing: .2em; }
.wisdom-card blockquote { margin: 28px 0 15px; font-family: var(--font-serif); font-size: 20px; line-height: 1.8; }
.wisdom-card small { color: rgba(255,255,255,.5); }
.wisdom-card i { position: absolute; right: -5px; bottom: -35px; color: rgba(255,255,255,.05); font-family: var(--font-serif); font-size: 150px; font-style: normal; }
@media (max-width: 1200px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .dashboard-grid { grid-template-columns: 1fr; } .side-stack { grid-template-columns: 1fr 1fr; } }
@media (max-width: 650px) { .stats-grid, .side-stack { grid-template-columns: 1fr; } .recent-panel { padding: 18px 12px; } }
</style>
