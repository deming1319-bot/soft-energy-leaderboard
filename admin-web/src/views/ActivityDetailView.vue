<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Edit, Refresh, Trophy } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { activityApi, submissionApi } from '@/api/services'
import StatusPill from '@/components/StatusPill.vue'
import type { ActivityDetail, Leaderboard, SubmissionGrade, SubmissionItem } from '@/types'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const loading = ref(true)
const tab = ref('submissions')
const detail = ref<ActivityDetail | null>(null)
const submissions = ref<SubmissionItem[]>([])
const leaderboard = ref<Leaderboard | null>(null)
const filter = ref('ALL')
const reviewOpen = ref(false)
const reviewSaving = ref(false)
const current = ref<SubmissionItem | null>(null)
const review = reactive<{ grade: SubmissionGrade; note: string }>({ grade: 'RUNNER_UP', note: '' })
const categoryLabel: Record<string, string> = { CLASSICS: '经典阅读', PHILOSOPHY: '经典哲思', LIFE_PRACTICE: '生活实践', CONFUCIAN: '传统文化', BUDDHIST: '待资质确认', TAOIST: '待资质确认', INTEGRATED: '待资质确认', MASTER_ORIGINAL: '待资质确认' }
const filtered = computed(() => submissions.value.filter((item) => filter.value === 'ALL' || item.grade === filter.value))
const counts = computed(() => submissions.value.reduce((acc, item) => { acc[item.grade] = (acc[item.grade] || 0) + 1; return acc }, {} as Record<string, number>))

function returnToList() { void router.push({ name: 'activities' }) }
async function load() { loading.value = true; try { [detail.value, submissions.value, leaderboard.value] = await Promise.all([activityApi.detail(id), activityApi.submissions(id), activityApi.leaderboard(id)]) } finally { loading.value = false } }
async function publish() { await ElMessageBox.confirm('发布后，学员将在开放时间内看到这道题。', '确认发布'); await activityApi.publish(id); ElMessage.success('发布成功'); await load() }
async function closeNow() { await ElMessageBox.confirm('截止后学员无法继续作答，并按规则展示榜单。', '确认截止', { type: 'warning' }); await activityApi.close(id); ElMessage.success('已截止'); await load() }
async function removeDraft() {
  if (!detail.value) return
  const confirmed = await ElMessageBox.confirm(
    `确定删除草稿“${detail.value.title}”吗？删除后无法恢复。`,
    '删除草稿题目',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  await activityApi.remove(id)
  ElMessage.success('草稿题目已删除')
  await router.replace({ name: 'activities' })
}
function openReview(item: SubmissionItem) { current.value = item; review.grade = item.grade === 'REVIEW_REQUIRED' ? 'RUNNER_UP' : item.grade; review.note = item.reviewNote || ''; reviewOpen.value = true }
async function saveReview() {
  if (!current.value) return
  if (!review.note.trim()) { ElMessage.warning('请填写人工复核依据'); return }
  reviewSaving.value = true
  try { await submissionApi.review(current.value.id, review.grade, review.note.trim()); ElMessage.success('复核结果已保存'); reviewOpen.value = false; await load() } finally { reviewSaving.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <template v-if="detail">
      <nav class="detail-navigation" aria-label="题目详情导航">
        <button type="button" class="back-to-list" @click="returnToList">
          <ArrowLeft aria-hidden="true" />
          <span>返回题目列表</span>
        </button>
        <span class="detail-navigation__divider" aria-hidden="true">/</span>
        <strong>{{ detail.title }}</strong>
      </nav>

      <div class="detail-hero">
        <div class="hero-pattern" aria-hidden="true" />
        <div class="hero-main"><div class="hero-meta"><span>{{ categoryLabel[detail.category] }}</span><StatusPill :value="detail.status" /></div><h1>{{ detail.title }}</h1><p>{{ detail.subtitle || '静心作答，于一问之间观照所学。' }}</p><div v-if="detail.deadlineMode === 'DURATION' && detail.status === 'DRAFT'" class="hero-time"><b>发布后立即开放</b><i>·</i><b>{{ detail.durationHours }} 小时后截止</b></div><div v-else class="hero-time"><b>{{ dayjs(detail.startAt).format('YYYY年MM月DD日 HH:mm') }}</b><i>至</i><b>{{ dayjs(detail.endAt).format('MM月DD日 HH:mm') }}</b></div></div>
        <div class="hero-actions"><el-button v-if="detail.status === 'DRAFT'" :icon="Edit" @click="router.push(`/activities/${id}/edit`)">编辑</el-button><el-button v-if="detail.status === 'DRAFT'" type="danger" plain :icon="Delete" @click="removeDraft">删除草稿</el-button><el-button v-if="detail.status === 'DRAFT'" type="primary" @click="publish">发布题目</el-button><el-button v-if="detail.status === 'PUBLISHED'" type="danger" plain @click="closeNow">提前截止</el-button></div>
      </div>

      <section class="metric-row">
        <div><span>答题人次</span><b>{{ submissions.length }}</b></div><div><span>冠军</span><b>{{ counts.CHAMPION || 0 }}</b></div><div><span>亚军</span><b>{{ counts.RUNNER_UP || 0 }}</b></div><div :class="{ alert: counts.REVIEW_REQUIRED }"><span>待复核</span><b>{{ counts.REVIEW_REQUIRED || 0 }}</b></div>
      </section>

      <section class="surface detail-panel">
        <el-tabs v-model="tab">
          <el-tab-pane label="答题记录" name="submissions">
            <div class="tab-toolbar"><div class="filters"><button v-for="item in [{v:'ALL',l:'全部'},{v:'CHAMPION',l:'冠军'},{v:'RUNNER_UP',l:'亚军'},{v:'REVIEW_REQUIRED',l:'待复核'},{v:'UNRANKED',l:'未入榜'}]" :key="item.v" :class="{ active: filter === item.v }" @click="filter = item.v">{{ item.l }}</button></div><el-button :icon="Refresh" text @click="load">刷新</el-button></div>
            <el-table :data="filtered" style="width:100%">
              <el-table-column label="学员" min-width="130"><template #default="scope"><div class="person"><span>{{ scope.row.userDisplayName.slice(0,1) }}</span><div><b>{{ scope.row.userDisplayName }}</b><small>{{ scope.row.maskedPhone }}</small></div></div></template></el-table-column>
              <el-table-column label="回答内容" min-width="310"><template #default="scope"><p class="answer-preview">{{ scope.row.answerText }}</p></template></el-table-column>
              <el-table-column label="匹配度 / 结果" width="145"><template #default="scope"><div class="grade-cell"><StatusPill :value="scope.row.grade" /><small>{{ Math.round(scope.row.score * 100) }}%</small></div></template></el-table-column>
              <el-table-column label="提交时间" width="150"><template #default="scope"><span class="muted">{{ dayjs(scope.row.submittedAt).format('MM-DD HH:mm:ss') }}</span></template></el-table-column>
              <el-table-column label="操作" width="90"><template #default="scope"><el-button link type="primary" @click="openReview(scope.row)">复核</el-button></template></el-table-column>
              <template #empty><el-empty description="暂无答题记录" /></template>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="荣誉榜单" name="leaderboard">
            <div v-if="leaderboard" class="leaderboards">
              <div v-if="!leaderboard.revealed" class="locked"><Trophy /><h3>榜单尚未揭晓</h3><p>截止后将按首次正确作答时间自动排列。</p></div>
              <template v-else>
                <div class="ranking champion-list"><header><span>冠</span><div><h3>冠军榜</h3><p>答案完全正确且顺序一致</p></div><b>{{ leaderboard.champions.length }} 人</b></header><ol><li v-for="item in leaderboard.champions" :key="item.userId"><i>{{ item.rank }}</i><span class="rank-avatar">{{ item.displayName.slice(0,1) }}</span><strong>{{ item.displayName }}</strong><time>{{ dayjs(item.submittedAt).format('MM-DD HH:mm:ss') }}</time></li></ol><el-empty v-if="!leaderboard.champions.length" description="暂无冠军" /></div>
                <div class="ranking runner-list"><header><span>亚</span><div><h3>亚军榜</h3><p>两句内容正确，但上下联整体颠倒</p></div><b>{{ leaderboard.runnersUp.length }} 人</b></header><ol><li v-for="item in leaderboard.runnersUp" :key="item.userId"><i>{{ item.rank }}</i><span class="rank-avatar">{{ item.displayName.slice(0,1) }}</span><strong>{{ item.displayName }}</strong><time>{{ dayjs(item.submittedAt).format('MM-DD HH:mm:ss') }}</time></li></ol><el-empty v-if="!leaderboard.runnersUp.length" description="暂无亚军" /></div>
              </template>
            </div>
          </el-tab-pane>
          <el-tab-pane label="题目与标准答案" name="question"><div class="question-grid"><article><span class="eyebrow">QUESTION</span><h3>题目正文</h3><p>{{ detail.questionText }}</p><small v-if="detail.sourceTitle">出处：{{ detail.sourceTitle }} {{ detail.sourceDetail }}</small></article><article class="secret"><span class="eyebrow">STANDARD ANSWER · 管理员可见</span><h3>标准答案</h3><p>{{ detail.standardAnswer }}</p><div class="points"><span v-for="(point,index) in detail.answerPoints" :key="point"><i>{{ index+1 }}</i>{{ point }}</span></div></article></div></el-tab-pane>
        </el-tabs>
      </section>
    </template>

    <el-dialog v-model="reviewOpen" title="人工复核答题" width="min(560px, 92vw)">
      <div v-if="current" class="review-dialog"><div class="review-answer"><span>{{ current.userDisplayName }} 的回答</span><p>{{ current.answerText }}</p></div><el-form label-position="top"><el-form-item label="最终评定"><el-radio-group v-model="review.grade"><el-radio-button value="CHAMPION">冠军</el-radio-button><el-radio-button value="RUNNER_UP">亚军</el-radio-button><el-radio-button value="UNRANKED">不入榜</el-radio-button></el-radio-group></el-form-item><el-form-item label="复核备注"><el-input v-model="review.note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明人工判断的依据" /></el-form-item></el-form></div>
      <template #footer><el-button @click="reviewOpen=false">取消</el-button><el-button type="primary" :loading="reviewSaving" @click="saveReview">确认评定</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-navigation { min-height: 40px; margin-bottom: 16px; display: flex; align-items: center; gap: 10px; color: var(--ink-500); }
.detail-navigation strong { max-width: min(52vw, 620px); overflow: hidden; color: var(--ink-650); font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.detail-navigation__divider { color: var(--paper-300); }
.back-to-list { min-height: 38px; padding: 8px 13px 8px 11px; display: inline-flex; align-items: center; gap: 8px; color: var(--ink-650); border: 1px solid var(--paper-300); border-radius: 10px; background: rgba(255,255,255,.72); box-shadow: 0 5px 15px rgba(61,54,45,.05); cursor: pointer; font: inherit; font-size: 12px; font-weight: 700; transition: color .18s ease, border-color .18s ease, background .18s ease, transform .18s ease; }
.back-to-list svg { width: 16px; height: 16px; }
.back-to-list:hover { color: var(--cinnabar-700); border-color: #d9b9b1; background: #fff; transform: translateX(-2px); }
.back-to-list:focus-visible { outline: 3px solid rgba(163,72,59,.18); outline-offset: 2px; }
.detail-hero { position: relative; min-height: 230px; padding: 34px 38px; display: flex; justify-content: space-between; gap: 25px; overflow: hidden; color: #fff; border-radius: var(--radius-xl); background: var(--pine-700); }
.hero-pattern { position:absolute; inset:0; opacity:.18; background: radial-gradient(circle at 78% 20%, #e0cc94 0 10%, transparent 10.5%), linear-gradient(145deg, transparent 58%, #92a397 58.3% 59%, transparent 59.3%); }
.hero-main,.hero-actions { position:relative; z-index:1; }
.hero-meta { display:flex; gap:10px; align-items:center; }.hero-meta>span:first-child { padding:5px 9px; border:1px solid rgba(255,255,255,.35); font-family:var(--font-serif);font-size:12px; }
.detail-hero h1 { margin:25px 0 9px; font-family:var(--font-serif);font-size:clamp(29px,3vw,43px);letter-spacing:.07em; }.detail-hero p { margin:0;color:rgba(255,255,255,.6); }
.hero-time { margin-top:27px;display:flex;gap:12px;align-items:center;font-size:12px; }.hero-time i { color:rgba(255,255,255,.35);font-style:normal; }.hero-actions { display:flex;align-items:flex-start;gap:10px; }
.metric-row { margin:-28px 30px 22px; position:relative; z-index:3; display:grid;grid-template-columns:repeat(4,1fr); background:#fff;border-radius:16px;box-shadow:var(--shadow-soft); }
.metric-row>div { padding:18px 24px;display:flex;align-items:center;justify-content:space-between;border-right:1px solid var(--paper-200); }.metric-row>div:last-child{border:0}.metric-row span{color:var(--ink-500);font-size:12px}.metric-row b{font-family:var(--font-serif);font-size:25px}.metric-row .alert b{color:var(--cinnabar-600)}
.detail-panel{padding:12px 24px 25px;overflow:hidden}.tab-toolbar{margin-bottom:10px;display:flex;justify-content:space-between;align-items:center}.filters{display:flex;gap:5px}.filters button{padding:8px 12px;border:0;border-radius:8px;color:var(--ink-500);background:var(--paper-100);cursor:pointer;font-size:12px}.filters button.active{color:#fff;background:var(--pine-600)}
.person{display:flex;align-items:center;gap:10px}.person>span,.rank-avatar{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;color:#fff;background:var(--pine-600);font-family:var(--font-serif)}.person div{display:grid;gap:2px}.person small{color:var(--ink-500)}.answer-preview{margin:0;max-width:600px;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;line-height:1.6}.grade-cell{display:flex;align-items:center;gap:7px}.grade-cell small{color:var(--ink-500)}
.leaderboards{min-height:280px}.locked{padding:60px 20px;text-align:center}.locked svg{width:45px;color:var(--gold-600)}.locked h3{font-family:var(--font-serif);font-size:23px}.locked p{color:var(--ink-500)}.leaderboards:has(.ranking){display:grid;grid-template-columns:1fr 1fr;gap:18px}.ranking{padding:22px;border:1px solid var(--paper-200);border-radius:16px}.ranking header{display:flex;gap:12px;align-items:center;padding-bottom:17px;border-bottom:1px solid var(--paper-200)}.ranking header>span{width:41px;height:41px;display:grid;place-items:center;border-radius:12px;background:var(--gold-100);color:var(--gold-600);font-family:var(--font-serif);font-size:20px}.runner-list header>span{color:#6e5b75;background:#eee8f0}.ranking h3{margin:0;font-family:var(--font-serif)}.ranking header p{margin:3px 0 0;color:var(--ink-500);font-size:10px}.ranking header>b{margin-left:auto;color:var(--ink-500);font-size:12px}.ranking ol{list-style:none;margin:12px 0 0;padding:0}.ranking li{padding:10px 4px;display:grid;grid-template-columns:27px 36px 1fr auto;gap:9px;align-items:center}.ranking li>i{font-style:normal;color:var(--gold-600);text-align:center}.ranking time{color:var(--ink-500);font-size:11px}
.question-grid{padding:14px 2px;display:grid;grid-template-columns:1fr 1fr;gap:20px}.question-grid article{padding:25px;border-radius:15px;background:var(--paper-100)}.question-grid .secret{background:#f5ebe7;border:1px solid #ead2cd}.question-grid h3{font-family:var(--font-serif);font-size:22px}.question-grid p{white-space:pre-wrap;line-height:1.9}.question-grid small{color:var(--ink-500)}.points{display:grid;gap:8px;margin-top:18px}.points span{display:flex;gap:8px;font-size:12px}.points i{width:20px;height:20px;display:grid;place-items:center;flex:0 0 auto;border-radius:50%;background:#fff;color:var(--cinnabar-600);font-style:normal}.review-answer{margin-bottom:20px;padding:16px;background:var(--paper-100);border-radius:12px}.review-answer span{color:var(--ink-500);font-size:11px}.review-answer p{margin:8px 0 0;line-height:1.8}
@media(max-width:850px){.detail-hero{flex-direction:column}.hero-actions{align-self:flex-start}.metric-row{margin:15px 0;grid-template-columns:1fr 1fr}.metric-row>div:nth-child(2){border-right:0}.leaderboards:has(.ranking),.question-grid{grid-template-columns:1fr}}
@media(max-width:560px){.detail-navigation__divider,.detail-navigation>strong{display:none}.detail-hero{padding:27px 20px}.metric-row{grid-template-columns:1fr 1fr}.metric-row>div{padding:15px}.detail-panel{padding-inline:12px}.filters{overflow-x:auto;max-width:80vw}.filters button{white-space:nowrap}.hero-actions{flex-wrap:wrap}}
</style>
