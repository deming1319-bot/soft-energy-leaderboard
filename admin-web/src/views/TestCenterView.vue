<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, Clock, Plus, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { testCenterApi } from '@/api/services'
import StatusPill from '@/components/StatusPill.vue'
import { mergeTestIdentities, type TestIdentity } from '@/utils/testIdentities'
import type {
  Leaderboard,
  SubmissionResult,
  TestActivityDetail,
  TestActivitySummary,
  TokenResponse,
} from '@/types'

const IDENTITIES_KEY = 'soft-energy-test-identities'
const SELECTED_KEY = 'soft-energy-test-selected'
const router = useRouter()
const categoryLabels: Record<string, string> = {
  CLASSICS: '经典阅读', PHILOSOPHY: '经典哲思', LIFE_PRACTICE: '生活实践', CONFUCIAN: '传统文化',
  BUDDHIST: '待资质确认', TAOIST: '待资质确认', INTEGRATED: '待资质确认', MASTER_ORIGINAL: '待资质确认',
}

function identitySuffix(testerKey: string): string {
  return testerKey.replace(/[^a-z0-9]/gi, '').slice(-4).toUpperCase()
}

function createIdentity(nickname?: string): TestIdentity {
  const testerKey = `tester-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 16)}`
  return {
    testerKey,
    nickname: nickname || `体验-${identitySuffix(testerKey)}`,
  }
}

function loadStoredIdentities(): unknown {
  try {
    return JSON.parse(localStorage.getItem(IDENTITIES_KEY) || '[]')
  } catch { /* 使用默认测试身份 */ }
  return null
}

const identities = ref<TestIdentity[]>([])
const selectedKey = ref('')
const newNickname = ref('')
const session = ref<TokenResponse | null>(null)
const activities = ref<TestActivitySummary[]>([])
const loading = ref(true)
const switching = ref(false)
const filter = ref<'ALL' | 'ACTIVE' | 'UPCOMING' | 'ENDED'>('ALL')
const filterOptions = [
  { value: 'ALL' as const, label: '全部' },
  { value: 'ACTIVE' as const, label: '进行中' },
  { value: 'UPCOMING' as const, label: '未开始' },
  { value: 'ENDED' as const, label: '已结束' },
]
const dialogOpen = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const selected = ref<TestActivityDetail | null>(null)
const result = ref<SubmissionResult | null>(null)
const board = ref<Leaderboard | null>(null)
const answer = ref('')

const currentIdentity = computed(() => identities.value.find((item) => item.testerKey === selectedKey.value) || identities.value[0])
const currentName = computed(() => session.value?.currentUser.displayName || (currentIdentity.value ? `测试学员·${currentIdentity.value.nickname}` : '测试学员加载中'))
const filteredActivities = computed(() => activities.value.filter((item) => filter.value === 'ALL' || item.timeState === filter.value))
const activeCount = computed(() => activities.value.filter((item) => item.timeState === 'ACTIVE').length)
const completedCount = computed(() => activities.value.filter((item) => item.submitted).length)
const formatTime = (value: string) => dayjs(value).format('MM-DD HH:mm')

function persistIdentities() {
  localStorage.setItem(IDENTITIES_KEY, JSON.stringify(identities.value))
  localStorage.setItem(SELECTED_KEY, selectedKey.value)
}

function studentToken(): string {
  if (!session.value?.accessToken) throw new Error('测试学员会话尚未就绪')
  return session.value.accessToken
}

async function loadActivities() {
  activities.value = await testCenterApi.activities(studentToken())
}

async function activateIdentity(key = selectedKey.value) {
  const identity = identities.value.find((item) => item.testerKey === key)
  if (!identity) return
  const changed = selectedKey.value !== identity.testerKey
  switching.value = true
  dialogOpen.value = false
  selectedKey.value = identity.testerKey
  persistIdentities()
  try {
    session.value = await testCenterApi.startSession(identity.nickname, identity.testerKey)
    await loadActivities()
    if (changed) ElMessage.success(`已切换为测试学员·${identity.nickname}`)
  } finally {
    switching.value = false
    loading.value = false
  }
}

async function bootstrapIdentities() {
  loading.value = true
  try {
    const defaults = await testCenterApi.accounts()
    identities.value = mergeTestIdentities(defaults, loadStoredIdentities())
    const savedSelectedKey = localStorage.getItem(SELECTED_KEY)
    selectedKey.value = identities.value.some((item) => item.testerKey === savedSelectedKey)
      ? savedSelectedKey as string
      : identities.value[0]?.testerKey || ''
    persistIdentities()
    await activateIdentity()
  } catch (error) {
    loading.value = false
    ElMessage.error(error instanceof Error ? error.message : '测试账号加载失败')
  }
}

async function addIdentity() {
  const nickname = newNickname.value.trim().replace(/^测试学员·/, '')
  if (!nickname) { ElMessage.warning('请先填写测试学员名称'); return }
  if (nickname.length > 24) { ElMessage.warning('测试学员名称不能超过24个字'); return }
  if (identities.value.some((item) => item.nickname === nickname)) {
    ElMessage.warning('这个测试学员已经存在，可以直接切换')
    return
  }
  const identity = createIdentity(nickname)
  identities.value.push(identity)
  newNickname.value = ''
  persistIdentities()
  await activateIdentity(identity.testerKey)
  ElMessage.success(`已切换为测试学员·${nickname}`)
}

async function openActivity(item: TestActivitySummary) {
  dialogOpen.value = true
  detailLoading.value = true
  selected.value = null
  result.value = null
  board.value = null
  answer.value = ''
  try {
    const token = studentToken()
    selected.value = await testCenterApi.detail(token, item.id)
    const requests: Promise<unknown>[] = [
      testCenterApi.leaderboard(token, item.id).then((value) => { board.value = value }),
    ]
    if (selected.value.submitted) {
      requests.push(testCenterApi.mySubmission(token, item.id).then((value) => { result.value = value }))
    }
    await Promise.all(requests)
  } finally {
    detailLoading.value = false
  }
}

async function submitAnswer() {
  if (!selected.value || !answer.value.trim()) { ElMessage.warning('请先写下答案'); return }
  try {
    await ElMessageBox.confirm(
      `将以“${currentName.value}”提交本次答案，提交后会占用一次作答机会。`,
      '确认提交答案',
      { confirmButtonText: '确认提交', cancelButtonText: '继续检查' },
    )
  } catch { return }
  submitting.value = true
  try {
    const token = studentToken()
    const id = selected.value.id
    const requestId = `web-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 14)}`
    result.value = await testCenterApi.submit(token, id, answer.value.trim(), requestId)
    selected.value = await testCenterApi.detail(token, id)
    board.value = await testCenterApi.leaderboard(token, id)
    answer.value = ''
    await loadActivities()
    ElMessage.success('答案已提交，判定结果已写入系统')
  } finally {
    submitting.value = false
  }
}

function openAdminRecords() {
  if (!selected.value) return
  dialogOpen.value = false
  router.push(`/activities/${selected.value.id}`)
}

onMounted(() => bootstrapIdentities())
</script>

<template>
  <div class="page-shell test-center-page">
    <div class="page-heading">
      <div><span class="eyebrow">SIMULATION LAB</span><h1>学员答题测试中心</h1><p>不经过小程序审核，也能按真实学员流程查看题目、提交答案并验证榜单。</p></div>
      <div class="page-actions"><el-button :icon="Refresh" :loading="loading || switching" @click="activateIdentity()">刷新题目</el-button></div>
    </div>

    <section class="lab-hero">
      <div class="lab-hero__copy">
        <span class="lab-seal">测</span>
        <div><small>当前模拟身份</small><h2>{{ currentName }}</h2><p>该身份拥有独立学员数据，提交结果会同步进入总览、答题记录、学员档案和荣誉榜单。</p></div>
      </div>
      <div class="identity-console">
        <div class="identity-console__head"><label>一键切换测试账号</label><span>默认 10 个独立账号</span></div>
        <div class="identity-grid">
          <button v-for="(item, index) in identities" :key="item.testerKey" type="button" :class="{ active: selectedKey === item.testerKey }" :disabled="switching" @click="activateIdentity(item.testerKey)">
            <i>{{ String(index + 1).padStart(2, '0') }}</i><b>{{ item.nickname }}</b><small>{{ selectedKey === item.testerKey ? '当前账号' : '点击切换' }}</small>
          </button>
        </div>
        <div class="identity-create"><el-input v-model="newNickname" maxlength="24" placeholder="例如：体验二号" @keyup.enter="addIdentity" /><el-button :icon="Plus" @click="addIdentity">新增身份</el-button></div>
      </div>
    </section>

    <section class="test-summary">
      <div><UserFilled /><span>测试身份<b>{{ identities.length }}</b></span></div>
      <i />
      <div><Clock /><span>进行中题目<b>{{ activeCount }}</b></span></div>
      <i />
      <div><CircleCheck /><span>当前身份已答<b>{{ completedCount }}</b></span></div>
      <p>测试提交使用正式判题规则，不会绕过时间和作答次数限制</p>
    </section>

    <section class="activity-section surface">
      <header>
        <div><span class="eyebrow">PRACTICE QUESTIONS</span><h3>可体验题目</h3></div>
        <div class="filters"><button v-for="item in filterOptions" :key="item.value" :class="{active:filter===item.value}" @click="filter=item.value">{{ item.label }}</button></div>
      </header>

      <div v-loading="loading || switching" class="question-grid">
        <article v-for="item in filteredActivities" :key="item.id" class="question-card" @click="openActivity(item)">
          <div class="question-card__top"><span>{{ categoryLabels[item.category] }}</span><StatusPill :value="item.timeState" /></div>
          <h4>{{ item.title }}</h4>
          <p>{{ item.subtitle || '静心读题，再落笔作答。' }}</p>
          <div class="question-card__meta"><span>{{ formatTime(item.startAt) }} 至 {{ formatTime(item.endAt) }}</span><span>{{ item.participantCount }} 人作答</span></div>
          <footer><b>{{ item.submitted ? '已作答 · 查看结果' : item.timeState === 'ACTIVE' ? '进入模拟作答' : item.timeState === 'ENDED' ? '查看结果与榜单' : '查看题目安排' }}</b><span>›</span></footer>
        </article>
        <el-empty v-if="!filteredActivities.length && !loading" description="当前没有符合条件的已发布题目" />
      </div>
    </section>

    <el-dialog v-model="dialogOpen" :title="selected?.title || '题目体验'" width="min(1040px, 94vw)" top="4vh" destroy-on-close class="test-dialog">
      <div v-loading="detailLoading" class="dialog-body">
        <template v-if="selected">
          <div class="dialog-context"><span>{{ categoryLabels[selected.category] }}</span><StatusPill :value="selected.timeState" /><small>{{ formatTime(selected.startAt) }} 至 {{ formatTime(selected.endAt) }}</small></div>
          <div class="answer-layout">
            <article class="question-paper">
              <div class="paper-label"><i>问</i><span>题目正文</span></div>
              <p>{{ selected.questionText }}</p>
              <div v-if="selected.sourceTitle" class="paper-source"><b>{{ selected.sourceTitle }}</b><span>{{ selected.sourceDetail }}</span></div>
              <div class="attempt-note">本身份已提交 {{ selected.attemptsUsed }} 次，还可提交 {{ Math.max(0, selected.maxAttempts - selected.attemptsUsed) }} 次</div>
            </article>

            <section class="answer-panel">
              <div v-if="result" class="result-card" :class="`result-${result.grade}`">
                <div><StatusPill :value="result.grade" /><strong>{{ Math.round(result.score * 100) }}%</strong></div>
                <p>{{ result.feedback }}</p>
                <small>提交于 {{ dayjs(result.submittedAt).format('YYYY-MM-DD HH:mm:ss') }}</small>
              </div>

              <template v-if="selected.canSubmit">
                <label>{{ result ? '继续作答' : '写下你的答案' }}</label>
                <el-input v-model="answer" type="textarea" :rows="9" maxlength="5000" show-word-limit placeholder="请在这里写下答案……" />
                <div class="submit-note">除表情符号外，系统会逐字核对两句内容、字数、标点和上下联顺序。</div>
                <el-button type="primary" size="large" :loading="submitting" @click="submitAnswer">完成并提交</el-button>
              </template>
              <div v-else-if="!result" class="unavailable-state"><span>{{ selected.timeState === 'UPCOMING' ? '候' : '止' }}</span><b>{{ selected.timeState === 'UPCOMING' ? '题目尚未开始' : '本题已经截止' }}</b><p>{{ selected.timeState === 'UPCOMING' ? '到达开放时间后即可模拟作答。' : '当前身份没有本题作答记录。' }}</p></div>

              <div v-if="result?.answerRevealed && result.standardAnswer" class="standard-answer"><span>标准答案 · 已揭晓</span><p>{{ result.standardAnswer }}</p></div>
              <div v-else-if="result" class="answer-locked">标准答案将在题目截止后揭晓</div>
            </section>
          </div>

          <section v-if="board" class="board-preview">
            <header><div><span class="eyebrow">HONOUR ROLL</span><h3>荣誉榜单</h3></div><el-button text type="primary" @click="openAdminRecords">前往后台答题记录</el-button></header>
            <div v-if="!board.revealed" class="board-locked"><span>候</span><div><b>榜单尚未揭晓</b><p>截止后冠军和亚军将按首次达到该等级的时间排列。</p></div></div>
            <div v-else class="board-columns">
              <div><h4>冠军榜 <span>{{ board.champions.length }}</span></h4><p v-for="entry in board.champions.slice(0,5)" :key="entry.userId" :class="{mine:entry.currentUser}"><i>{{ entry.rank }}</i><b>{{ entry.displayName }}</b><small>{{ dayjs(entry.submittedAt).format('HH:mm:ss') }}</small></p><em v-if="!board.champions.length">暂无冠军</em></div>
              <div><h4>亚军榜 <span>{{ board.runnersUp.length }}</span></h4><p v-for="entry in board.runnersUp.slice(0,5)" :key="entry.userId" :class="{mine:entry.currentUser}"><i>{{ entry.rank }}</i><b>{{ entry.displayName }}</b><small>{{ dayjs(entry.submittedAt).format('HH:mm:ss') }}</small></p><em v-if="!board.runnersUp.length">暂无亚军</em></div>
            </div>
          </section>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.lab-hero{position:relative;margin-bottom:18px;padding:28px 30px;display:grid;grid-template-columns:minmax(280px,.72fr) minmax(560px,1.28fr);gap:30px;overflow:hidden;color:#fff;border-radius:var(--radius-lg);background:#293b32;box-shadow:0 18px 40px rgba(34,48,41,.13)}.lab-hero::after{content:'';position:absolute;right:38%;bottom:-145px;width:250px;height:250px;border:1px solid rgba(214,199,157,.18);border-radius:50%}.lab-hero__copy{position:relative;z-index:1;display:flex;align-items:center;gap:20px}.lab-seal{width:68px;height:68px;display:grid;place-items:center;flex:0 0 auto;border:1px solid rgba(231,216,177,.48);color:#e2d3aa;font-family:var(--font-serif);font-size:32px}.lab-hero__copy small{color:#cdbc91;font-size:10px;letter-spacing:.2em}.lab-hero h2{margin:6px 0 5px;font-family:var(--font-serif);font-size:27px;letter-spacing:.06em}.lab-hero p{max-width:590px;margin:0;color:rgba(255,255,255,.58);font-size:12px;line-height:1.7}.identity-console{position:relative;z-index:1;padding:18px;border:1px solid rgba(255,255,255,.1);border-radius:14px;background:rgba(255,255,255,.055)}.identity-console__head{margin-bottom:10px;display:flex;align-items:center;justify-content:space-between;gap:12px}.identity-console__head label{color:rgba(255,255,255,.76);font-size:11px;font-weight:700}.identity-console__head span{color:#cdbc91;font-size:9px}.identity-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:7px}.identity-grid button{min-width:0;padding:9px 7px;display:grid;grid-template-columns:auto 1fr;align-items:center;gap:2px 7px;color:rgba(255,255,255,.68);border:1px solid rgba(255,255,255,.1);border-radius:9px;background:rgba(255,255,255,.045);cursor:pointer;text-align:left;transition:.18s ease}.identity-grid button:hover:not(:disabled){border-color:rgba(224,204,150,.48);background:rgba(255,255,255,.09)}.identity-grid button.active{color:#273a30;border-color:#d9c693;background:#e3d4aa;box-shadow:0 7px 18px rgba(0,0,0,.13)}.identity-grid button:disabled{cursor:wait;opacity:.65}.identity-grid i{grid-row:1/3;width:25px;height:25px;display:grid;place-items:center;border-radius:7px;color:#d9c693;background:rgba(0,0,0,.17);font-size:9px;font-style:normal}.identity-grid button.active i{color:#fff;background:#9d493d}.identity-grid b{overflow:hidden;font-size:10px;text-overflow:ellipsis;white-space:nowrap}.identity-grid small{font-size:8px;opacity:.66}.identity-create{margin-top:9px;display:grid;grid-template-columns:1fr auto;gap:8px}.test-summary{margin-bottom:18px;padding:18px 24px;display:flex;align-items:center;gap:21px;border:1px solid var(--paper-300);border-radius:var(--radius-lg);background:#fbfaf6}.test-summary>div{display:flex;align-items:center;gap:10px}.test-summary svg{width:24px;color:var(--cinnabar-600)}.test-summary span{display:grid;color:var(--ink-500);font-size:10px}.test-summary b{color:var(--ink-950);font-family:var(--font-serif);font-size:23px}.test-summary>i{width:1px;height:34px;background:var(--paper-300)}.test-summary>p{margin-left:auto;color:var(--ink-500);font-size:11px}.activity-section{padding:23px 25px 28px}.activity-section>header{margin-bottom:19px;display:flex;align-items:end;justify-content:space-between}.activity-section h3,.board-preview h3{margin:5px 0 0;font-family:var(--font-serif);font-size:21px}.filters{padding:4px;display:flex;gap:3px;border-radius:10px;background:var(--paper-100)}.filters button{padding:8px 13px;border:0;border-radius:8px;color:var(--ink-500);background:transparent;cursor:pointer;font-size:11px}.filters button.active{color:var(--cinnabar-700);background:#fff;box-shadow:0 3px 10px rgba(61,54,45,.09);font-weight:700}.question-grid{min-height:180px;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.question-card{padding:20px 20px 0;overflow:hidden;border:1px solid var(--paper-300);border-radius:15px;background:#fff;cursor:pointer;transition:.2s ease}.question-card:hover{transform:translateY(-2px);border-color:#d3b9ae;box-shadow:var(--shadow-soft)}.question-card__top{display:flex;align-items:center;justify-content:space-between}.question-card__top>span{color:var(--cinnabar-600);font-size:10px;font-weight:700;letter-spacing:.12em}.question-card h4{margin:18px 0 8px;font-family:var(--font-serif);font-size:19px;line-height:1.45}.question-card>p{min-height:42px;margin:0;color:var(--ink-500);font-size:12px;line-height:1.7}.question-card__meta{margin:17px 0;display:flex;justify-content:space-between;color:var(--ink-500);font-size:10px}.question-card footer{margin:0 -20px;padding:14px 20px;display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--paper-200);background:#fcfbf8}.question-card footer b{color:var(--pine-700);font-size:12px}.question-card footer span{color:var(--cinnabar-600);font-size:22px}.dialog-body{min-height:240px}.dialog-context{margin-bottom:15px;display:flex;align-items:center;gap:10px}.dialog-context>span{color:var(--cinnabar-600);font-size:11px;font-weight:700}.dialog-context small{margin-left:auto;color:var(--ink-500)}.answer-layout{display:grid;grid-template-columns:minmax(0,1.06fr) minmax(360px,.94fr);gap:16px}.question-paper,.answer-panel{padding:25px;border:1px solid var(--paper-300);border-radius:15px;background:#fff}.paper-label{display:flex;align-items:center;gap:10px;color:var(--ink-500);font-size:11px;letter-spacing:.12em}.paper-label i{width:30px;height:30px;display:grid;place-items:center;color:#fff;border-radius:50%;background:var(--pine-700);font-family:var(--font-serif);font-size:16px;font-style:normal}.question-paper>p{margin:22px 0;color:var(--ink-950);font-family:var(--font-serif);font-size:18px;line-height:2;white-space:pre-wrap}.paper-source{padding:14px 16px;display:grid;gap:4px;border-left:3px solid var(--gold-500);background:var(--paper-100)}.paper-source b{font-size:12px}.paper-source span{color:var(--ink-500);font-size:10px}.attempt-note{margin-top:20px;color:var(--ink-500);font-size:11px}.answer-panel>label{display:block;margin:2px 0 10px;font-weight:700;font-size:13px}.submit-note{margin:10px 0 14px;color:var(--ink-500);font-size:10px}.answer-panel>.el-button{width:100%}.result-card{margin-bottom:18px;padding:16px 18px;border-radius:12px;background:var(--paper-100)}.result-card>div{display:flex;align-items:center;justify-content:space-between}.result-card strong{font-family:var(--font-serif);font-size:28px}.result-card p{margin:10px 0 7px;font-size:12px;line-height:1.65}.result-card small{color:var(--ink-500);font-size:10px}.result-CHAMPION{background:#f6f0df}.result-RUNNER_UP{background:#f0ebf2}.result-REVIEW_REQUIRED{background:#f6eae7}.standard-answer{margin-top:18px;padding:17px;border-left:3px solid var(--gold-500);background:#f8f4e9}.standard-answer span{color:#8a6432;font-size:10px;font-weight:700;letter-spacing:.08em}.standard-answer p{margin:8px 0 0;font-family:var(--font-serif);font-size:13px;line-height:1.8;white-space:pre-wrap}.answer-locked{margin-top:17px;padding:13px;color:var(--ink-500);border-radius:10px;background:var(--paper-100);font-size:11px;text-align:center}.unavailable-state{min-height:210px;display:grid;place-items:center;align-content:center;text-align:center}.unavailable-state>span{width:56px;height:56px;display:grid;place-items:center;border:1px solid var(--paper-300);border-radius:50%;font-family:var(--font-serif);font-size:22px}.unavailable-state b{margin-top:12px}.unavailable-state p{margin:5px 0;color:var(--ink-500);font-size:11px}.board-preview{margin-top:16px;padding:20px 22px;border:1px solid var(--paper-300);border-radius:15px;background:#fff}.board-preview>header{display:flex;align-items:center;justify-content:space-between}.board-locked{padding:18px;display:flex;align-items:center;gap:14px;border-radius:12px;background:var(--paper-100)}.board-locked>span{width:43px;height:43px;display:grid;place-items:center;border-radius:50%;color:#fff;background:var(--pine-600);font-family:var(--font-serif)}.board-locked b{font-size:13px}.board-locked p{margin:4px 0 0;color:var(--ink-500);font-size:11px}.board-columns{margin-top:15px;display:grid;grid-template-columns:1fr 1fr;gap:14px}.board-columns>div{padding:16px;border-radius:12px;background:var(--paper-100)}.board-columns h4{margin:0 0 10px;font-family:var(--font-serif)}.board-columns h4 span{margin-left:5px;color:var(--ink-500);font-size:11px}.board-columns p{margin:5px 0;padding:8px 9px;display:grid;grid-template-columns:25px 1fr auto;align-items:center;border-radius:8px;background:#fff}.board-columns p.mine{box-shadow:inset 3px 0 var(--cinnabar-500)}.board-columns p i{font-style:normal;color:var(--cinnabar-600);font-weight:700}.board-columns p b{font-size:11px}.board-columns p small{color:var(--ink-500);font-size:9px}.board-columns em{display:block;padding:10px;color:var(--ink-500);font-size:11px;font-style:normal;text-align:center}
@media(max-width:1100px){.question-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.lab-hero{grid-template-columns:1fr}.test-summary>p{display:none}}
@media(max-width:760px){.lab-hero{padding:22px 18px}.lab-hero__copy{align-items:flex-start}.lab-seal{width:52px;height:52px}.lab-hero h2{font-size:22px;letter-spacing:.03em}.identity-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.identity-create{grid-template-columns:1fr}.test-summary{display:grid;grid-template-columns:1fr 1fr}.test-summary>i{display:none}.activity-section{padding:18px 14px}.activity-section>header{align-items:flex-start;flex-direction:column;gap:13px}.filters{max-width:100%;overflow-x:auto}.filters button{white-space:nowrap}.question-grid{grid-template-columns:1fr}.answer-layout,.board-columns{grid-template-columns:1fr}.question-paper,.answer-panel{padding:20px 17px}.dialog-context{align-items:flex-start;flex-wrap:wrap}.dialog-context small{width:100%;margin-left:0}}
</style>
