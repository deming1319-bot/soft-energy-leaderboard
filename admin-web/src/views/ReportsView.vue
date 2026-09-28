<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ChatDotSquare, CircleCheck, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { reportApi } from '@/api/services'
import StatusPill from '@/components/StatusPill.vue'
import type { ModerationAction, ReportItem, ReportStatus, ReportType } from '@/types'

const status = ref<'ALL' | ReportStatus>('ALL')
const loading = ref(true)
const saving = ref(false)
const rows = ref<ReportItem[]>([])
const current = ref<ReportItem | null>(null)
const dialogVisible = ref(false)
const form = reactive<{ status: ReportStatus; note: string; action: ModerationAction }>({
  status: 'PROCESSING', note: '', action: 'NONE',
})

const typeLabels: Record<ReportType, string> = {
  ILLEGAL_CONTENT: '违法不良信息', INFRINGEMENT: '侵权投诉', INAPPROPRIATE_NICKNAME: '昵称不当',
  QUESTION_CONTENT: '题目内容', SCORE_APPEAL: '评分申诉', PRIVACY: '隐私问题', OTHER: '其他反馈',
}
const targetLabels: Record<string, string> = { GENERAL: '平台服务', ACTIVITY: '题目', USER: '榜单昵称', SUBMISSION: '答题记录' }
const pendingCount = computed(() => rows.value.filter((item) => item.status === 'PENDING').length)
const processingCount = computed(() => rows.value.filter((item) => item.status === 'PROCESSING').length)
const closedCount = computed(() => rows.value.filter((item) => item.status === 'RESOLVED' || item.status === 'REJECTED').length)

async function loadData() {
  loading.value = true
  try { rows.value = await reportApi.list(status.value === 'ALL' ? undefined : status.value) }
  finally { loading.value = false }
}

function openHandle(item: ReportItem) {
  current.value = item
  form.status = item.status === 'PENDING' ? 'PROCESSING' : item.status
  form.note = item.handlingNote || ''
  form.action = 'NONE'
  dialogVisible.value = true
}

async function saveHandle() {
  if (!current.value) return
  if ((form.status === 'RESOLVED' || form.status === 'REJECTED') && form.note.trim().length < 2) {
    ElMessage.warning('办结或驳回时请填写处理说明')
    return
  }
  saving.value = true
  try {
    await reportApi.handle(current.value.id, form.status, form.note.trim(), form.action)
    ElMessage.success('处理结果已保存')
    dialogVisible.value = false
    await loadData()
  } finally { saving.value = false }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-heading">
      <div><span class="eyebrow">REPORT CENTRE</span><h1>反馈与举报</h1><p>集中受理内容投诉、昵称举报、评分申诉和隐私问题，所有处理动作保留审计记录。</p></div>
      <el-button :loading="loading" @click="loadData">刷新工单</el-button>
    </div>

    <section class="summary-grid">
      <article><span class="summary-icon pending"><ChatDotSquare /></span><div><small>待处理</small><b>{{ pendingCount }}</b></div></article>
      <article><span class="summary-icon processing"><Clock /></span><div><small>处理中</small><b>{{ processingCount }}</b></div></article>
      <article><span class="summary-icon resolved"><CircleCheck /></span><div><small>已归档</small><b>{{ closedCount }}</b></div></article>
      <p>建议收到后尽快确认，普通反馈在 7 个工作日内给出处理结果。</p>
    </section>

    <section class="surface report-panel">
      <header>
        <div><h3>工单列表</h3><span>按创建时间倒序排列</span></div>
        <el-segmented v-model="status" :options="[
          { label: '全部', value: 'ALL' }, { label: '待处理', value: 'PENDING' },
          { label: '处理中', value: 'PROCESSING' }, { label: '已办结', value: 'RESOLVED' },
          { label: '已驳回', value: 'REJECTED' },
        ]" @change="loadData" />
      </header>
      <div class="desktop-report-table">
        <el-table v-loading="loading" :data="rows" style="width:100%">
          <el-table-column label="反馈类型" width="145"><template #default="scope"><div class="type-cell"><b>{{ typeLabels[scope.row.type as ReportType] }}</b><small>{{ targetLabels[scope.row.targetType] }}</small></div></template></el-table-column>
          <el-table-column label="反馈对象" min-width="170"><template #default="scope"><div class="target-cell"><b>{{ scope.row.targetLabel }}</b><small>提交人：{{ scope.row.reporterDisplayName }}</small></div></template></el-table-column>
          <el-table-column label="情况说明" min-width="300"><template #default="scope"><p class="description">{{ scope.row.description }}</p></template></el-table-column>
          <el-table-column label="提交时间" width="160"><template #default="scope"><span class="muted">{{ dayjs(scope.row.createdAt).format('YYYY-MM-DD HH:mm') }}</span></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="scope"><StatusPill :value="scope.row.status" /></template></el-table-column>
          <el-table-column label="操作" width="104" fixed="right"><template #default="scope"><el-button link type="primary" @click="openHandle(scope.row)">查看处理</el-button></template></el-table-column>
          <template #empty><el-empty description="当前没有反馈工单" /></template>
        </el-table>
      </div>

      <div v-loading="loading" class="mobile-report-list">
        <article v-for="item in rows" :key="item.id" class="report-card">
          <header><div><span>{{ typeLabels[item.type] }}</span><small>{{ targetLabels[item.targetType] }}</small></div><StatusPill :value="item.status" /></header>
          <h4>{{ item.targetLabel }}</h4>
          <p>{{ item.description }}</p>
          <div class="report-card__meta"><span>提交人：{{ item.reporterDisplayName }}</span><time>{{ dayjs(item.createdAt).format('MM-DD HH:mm') }}</time></div>
          <button type="button" @click="openHandle(item)">查看并处理</button>
        </article>
        <div v-if="!loading && !rows.length" class="mobile-report-empty">
          <span><ChatDotSquare /></span><b>当前没有反馈工单</b><p>新的举报、申诉或隐私请求会显示在这里。</p>
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="处理反馈工单" width="min(620px, 92vw)" destroy-on-close>
      <div v-if="current" class="handle-dialog">
        <section><span>{{ typeLabels[current.type] }}</span><b>{{ current.targetLabel }}</b><p>{{ current.description }}</p></section>
        <el-form label-position="top">
          <el-form-item label="处理状态"><el-radio-group v-model="form.status"><el-radio-button value="PROCESSING">处理中</el-radio-button><el-radio-button value="RESOLVED">已办结</el-radio-button><el-radio-button value="REJECTED">已驳回</el-radio-button></el-radio-group></el-form-item>
          <el-form-item v-if="current.targetType === 'USER'" label="账号处置"><el-select v-model="form.action" style="width:100%"><el-option label="不执行账号操作" value="NONE"/><el-option label="重置违规昵称" value="RESET_NICKNAME"/><el-option label="停用该账号" value="DISABLE_USER"/></el-select></el-form-item>
          <el-form-item label="处理说明"><el-input v-model="form.note" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="写明核查结论、已经采取的措施或驳回原因" /></el-form-item>
        </el-form>
        <div class="dialog-note">处理结果会在用户的“反馈与举报”页面展示；账号处置会同步写入操作记录。</div>
      </div>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveHandle">保存处理结果</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.summary-grid{margin-bottom:20px;display:grid;grid-template-columns:repeat(3,minmax(150px,220px)) 1fr;gap:14px}.summary-grid article{min-height:94px;padding:19px 21px;display:flex;align-items:center;gap:14px;color:#fff;border-radius:var(--radius-lg);background:var(--pine-700)}.summary-icon{width:43px;height:43px;display:grid;place-items:center;border-radius:13px;background:rgba(255,255,255,.1)}.summary-icon svg{width:21px}.summary-grid article div{display:grid;gap:2px}.summary-grid small{color:rgba(255,255,255,.58);font-size:11px}.summary-grid b{font-family:var(--font-serif);font-size:28px}.summary-grid>p{margin:0;padding:20px 22px;display:flex;align-items:center;color:var(--ink-500);border:var(--border-subtle);border-radius:var(--radius-lg);background:rgba(255,255,255,.55);font-size:12px;line-height:1.7}.report-panel{padding:13px 18px 20px;overflow:hidden}.report-panel>header{padding:8px 3px 19px;display:flex;align-items:center;justify-content:space-between;gap:18px}.report-panel h3{margin:0 0 4px;font-family:var(--font-serif);font-size:20px}.report-panel>header span{color:var(--ink-500);font-size:11px}.type-cell,.target-cell{display:grid;gap:4px}.type-cell b,.target-cell b{font-size:13px}.type-cell small,.target-cell small{color:var(--ink-500);font-size:10px}.description{margin:0;display:-webkit-box;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:2;color:var(--ink-650);font-size:12px;line-height:1.65}.handle-dialog>section{margin-bottom:22px;padding:18px;border-radius:14px;background:var(--paper-100)}.handle-dialog>section span{display:block;margin-bottom:6px;color:var(--cinnabar-600);font-size:11px;font-weight:700;letter-spacing:.08em}.handle-dialog>section b{font-family:var(--font-serif);font-size:18px}.handle-dialog>section p{margin:10px 0 0;color:var(--ink-650);font-size:13px;line-height:1.75;white-space:pre-wrap}.dialog-note{padding:12px 14px;color:var(--ink-500);border-left:3px solid var(--gold-600);background:var(--gold-100);font-size:11px;line-height:1.65}.mobile-report-list{display:none}@media(max-width:900px){.summary-grid{grid-template-columns:repeat(3,1fr)}.summary-grid>p{grid-column:1/-1}.report-panel>header{align-items:flex-start;flex-direction:column}}@media(max-width:600px){.summary-grid{grid-template-columns:1fr}.summary-grid>p{grid-column:auto}.report-panel{padding:13px 10px 12px}.report-panel>header{padding:8px 4px 15px}.report-panel>header :deep(.el-segmented){width:100%;max-width:100%;overflow:hidden}.report-panel>header :deep(.el-segmented__item){min-width:0;padding-inline:8px;flex:1}.desktop-report-table{display:none}.mobile-report-list{min-height:220px;display:grid;gap:11px}.report-card{padding:16px;border:1px solid var(--paper-200);border-radius:15px;background:#fff;box-shadow:0 6px 18px rgba(48,43,36,.04)}.report-card>header{padding:0;display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.report-card>header>div{display:grid;gap:4px}.report-card>header span{color:var(--cinnabar-600);font-size:11px;font-weight:700}.report-card>header small{color:var(--ink-500);font-size:10px}.report-card h4{margin:15px 0 7px;font-family:var(--font-serif);font-size:18px}.report-card>p{margin:0;display:-webkit-box;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:3;color:var(--ink-650);font-size:12px;line-height:1.7}.report-card__meta{margin-top:14px;padding-top:12px;display:flex;justify-content:space-between;gap:12px;color:var(--ink-500);border-top:1px solid var(--paper-200);font-size:10px}.report-card>button{width:100%;height:39px;margin-top:13px;color:var(--cinnabar-700);border:0;border-radius:9px;background:#f5e8e4;cursor:pointer;font-size:12px;font-weight:700}.mobile-report-empty{min-height:250px;padding:28px 18px;display:grid;place-items:center;align-content:center;text-align:center}.mobile-report-empty>span{width:56px;height:56px;display:grid;place-items:center;color:var(--pine-700);border-radius:50%;background:var(--pine-100)}.mobile-report-empty svg{width:25px}.mobile-report-empty>b{margin-top:14px;font-family:var(--font-serif);font-size:18px}.mobile-report-empty p{margin:6px 0;color:var(--ink-500);font-size:11px;line-height:1.7}}@media(max-width:360px){.report-panel>header :deep(.el-segmented__item){padding-inline:2px;font-size:11px}}
</style>
