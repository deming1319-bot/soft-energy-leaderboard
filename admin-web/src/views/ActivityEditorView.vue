<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { activityApi } from '@/api/services'
import type { ActivityPayload, CultureCategory } from '@/types'

const route = useRoute()
const router = useRouter()
const id = computed(() => typeof route.params.id === 'string' ? route.params.id : '')
const editing = computed(() => Boolean(id.value))
const saving = ref(false)
const loading = ref(editing.value)
const timeRange = ref<[Date, Date]>([dayjs().toDate(), dayjs().add(12, 'hour').toDate()])
const form = reactive<ActivityPayload>({ title: '', subtitle: '', category: 'CLASSICS', questionText: '', sourceTitle: '', sourceDetail: '', standardAnswer: '', answerPoints: ['', ''], runnerUpThreshold: 0.9, deadlineMode: 'DURATION', durationHours: 12, startAt: '', endAt: '', revealMode: 'AFTER_DEADLINE', maxAttempts: 1 })

onMounted(async () => {
  if (!editing.value) return
  try { const result = await activityApi.detail(id.value); Object.assign(form, result); timeRange.value = [new Date(result.startAt), new Date(result.endAt)] } finally { loading.value = false }
})
async function save(publish = false) {
  if (!form.title.trim() || !form.questionText.trim() || !form.standardAnswer.trim()) { ElMessage.warning('请填写题目名称、考题内容和标准答案'); return }
  const answerLines = form.standardAnswer.replace(/\r\n/g, '\n').split('\n').map((line) => line.trim()).filter(Boolean)
  if (answerLines.length !== 2) { ElMessage.warning('标准答案必须正好两行：第一行上联或第一句，第二行下联或第二句'); return }
  form.standardAnswer = answerLines.join('\n')
  form.answerPoints = answerLines
  form.runnerUpThreshold = 0.9
  if (form.deadlineMode === 'DURATION') {
    const durationHours = Number(form.durationHours)
    if (!Number.isInteger(durationHours) || durationHours < 1 || durationHours > 720) { ElMessage.warning('答题时长必须填写1至720之间的整数小时'); return }
    const start = dayjs()
    form.durationHours = durationHours
    form.startAt = start.toISOString()
    form.endAt = start.add(durationHours, 'hour').toISOString()
  } else {
    if (!timeRange.value?.[0] || !timeRange.value?.[1]) { ElMessage.warning('请选择完整的开始和截止时间'); return }
    form.durationHours = null
    form.startAt = dayjs(timeRange.value[0]).toISOString()
    form.endAt = dayjs(timeRange.value[1]).toISOString()
  }
  saving.value = true
  try {
    const saved = editing.value ? await activityApi.update(id.value, form) : await activityApi.create(form)
    if (publish) await activityApi.publish(saved.id)
    ElMessage.success(publish ? '题目已保存并发布' : '草稿已保存')
    await router.push(`/activities/${saved.id}`)
  } finally { saving.value = false }
}
</script>

<template>
  <div class="page-shell editor-page" v-loading="loading">
    <div class="page-heading"><div><span class="eyebrow">QUESTION EDITOR</span><h1>{{ editing ? '编辑修习题目' : '创建修习题目' }}</h1><p>标准答案只用于系统判题，发布后不会在截止前展示给学员。</p></div><div class="page-actions"><el-button @click="router.back()">取消</el-button><el-button :loading="saving" @click="save(false)">保存草稿</el-button><el-button type="primary" :loading="saving" @click="save(true)">保存并发布</el-button></div></div>

    <div class="editor-grid">
      <section class="surface form-surface">
        <header><span>一</span><div><h3>题目内容</h3><p>学员在小程序中直接看到的内容</p></div></header>
        <el-form label-position="top">
          <div class="two-cols"><el-form-item label="题目名称 *"><el-input v-model="form.title" maxlength="120" show-word-limit placeholder="例如：何谓知行合一" /></el-form-item><el-form-item label="文化分类"><el-select v-model="form.category" style="width:100%"><el-option v-for="item in [{v:'CLASSICS',l:'经典阅读'},{v:'PHILOSOPHY',l:'经典哲思'},{v:'LIFE_PRACTICE',l:'生活实践'},{v:'CONFUCIAN',l:'传统文化'}]" :key="item.v" :label="item.l" :value="item.v as CultureCategory" /></el-select><div class="release-policy">首版仅发布传统文化、经典哲思与生活实践内容；涉及宗教教义或师门讲解的内容需完成资质确认后另行开放。</div></el-form-item></div>
          <el-form-item label="引导副标题"><el-input v-model="form.subtitle" maxlength="200" placeholder="用一句话帮助学员进入思考" /></el-form-item>
          <el-form-item label="考题内容 *"><el-input v-model="form.questionText" type="textarea" :rows="7" maxlength="5000" show-word-limit placeholder="清晰描述题目、背景与作答要求……" /></el-form-item>
          <div class="two-cols"><el-form-item label="出处"><el-input v-model="form.sourceTitle" placeholder="例如：《传习录》" /></el-form-item><el-form-item label="出处说明"><el-input v-model="form.sourceDetail" placeholder="章节、版本或内容整理说明" /></el-form-item></div>
        </el-form>
      </section>

      <section class="surface form-surface answer-surface">
        <header><span>二</span><div><h3>判题依据</h3><p>仅管理员可见，请准确录入</p></div><i>保密</i></header>
        <el-form label-position="top">
          <el-form-item label="标准答案（两行，顺序即判题顺序）*"><el-input v-model="form.standardAnswer" type="textarea" :rows="7" maxlength="5000" show-word-limit placeholder="第一行：上联或第一句&#10;第二行：下联或第二句" /><div class="answer-format-tip">必须正好填写两行。除表情符号外，文字、字数、标点和顺序都会参与精确比较。</div></el-form-item>
          <div class="exact-rules"><div><i>冠</i><span><b>冠军</b><small>去掉表情后，两行逐字且顺序完全一致</small></span></div><div><i>亚</i><span><b>亚军</b><small>两行文字都正确，但上下联整体颠倒</small></span></div><div><i>止</i><span><b>不入榜</b><small>少字、多字、错字、标点不同或内容不完整</small></span></div></div>
          <el-form-item label="每人最多作答"><el-input-number v-model="form.maxAttempts" :min="1" :max="5" style="width:100%" /></el-form-item>
        </el-form>
      </section>

      <section class="surface form-surface full-width">
        <header><span>三</span><div><h3>开放与揭榜</h3><p>控制学员可见时间和标准答案展示时机</p></div></header>
        <el-form label-position="top">
          <el-form-item label="截止方式"><el-radio-group v-model="form.deadlineMode"><el-radio-button value="DURATION">发布后按时长截止</el-radio-button><el-radio-button value="FIXED_TIME">指定日期时间截止</el-radio-button></el-radio-group></el-form-item>
          <div class="schedule-grid">
            <el-form-item v-if="form.deadlineMode === 'DURATION'" label="答题开放时长 *"><div class="duration-field"><el-input-number v-model="form.durationHours" :min="1" :max="720" :step="1" /><span>小时</span></div><div class="schedule-tip">默认12小时。题目点击发布的那一刻开始计时，可改为24小时或其他整数小时。</div></el-form-item>
            <el-form-item v-else label="开始与截止日期时间 *"><el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="截止时间" style="width:100%" /><div class="schedule-tip">适合指定在第二天或某一天的具体时刻截止；到达截止时间后系统立即拒绝新提交。</div></el-form-item>
            <el-form-item label="标准答案展示"><el-radio-group v-model="form.revealMode"><el-radio-button value="AFTER_DEADLINE">截止后展示</el-radio-button><el-radio-button value="AFTER_SUBMIT">提交后展示</el-radio-button></el-radio-group></el-form-item>
          </div>
        </el-form>
        <div class="policy-note"><b>排名规则</b><span>冠军和亚军分别编号；同一等级按首次达到该等级的服务器提交时间升序排列。没有上下联颠倒者时，亚军榜保持为空。</span></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.editor-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-surface { padding: 27px; }
.form-surface > header { margin-bottom: 25px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--paper-200); padding-bottom: 18px; }
.form-surface > header > span { width: 34px; height: 34px; display: grid; place-items: center; color: var(--cinnabar-700); background: #f4e7e3; border-radius: 10px; font-family: var(--font-serif); }
.form-surface h3 { margin: 0; font-family: var(--font-serif); font-size: 20px; }
.form-surface header p { margin: 3px 0 0; color: var(--ink-500); font-size: 11px; }
.form-surface header i { margin-left: auto; padding: 5px 8px; border: 1px solid #e4c4bf; color: var(--cinnabar-600); font-size: 10px; font-style: normal; letter-spacing: .15em; }
.two-cols { display: grid; grid-template-columns: 1.3fr .7fr; gap: 16px; }
.release-policy { margin-top: 8px; color: var(--warning-600); font-size: 10px; line-height: 1.55; }
.answer-format-tip,.schedule-tip { margin-top: 8px; color: var(--ink-500); font-size: 11px; line-height: 1.6; }
.exact-rules { margin: -2px 0 20px; display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; }
.exact-rules>div { padding: 13px; display: flex; align-items: flex-start; gap: 10px; border: 1px solid var(--paper-200); border-radius: 12px; background: var(--paper-100); }
.exact-rules i { width: 29px; height: 29px; display: grid; place-items: center; flex: 0 0 auto; color: var(--cinnabar-700); border-radius: 9px; background: #f2dfda; font-family: var(--font-serif); font-style: normal; }
.exact-rules span { display: grid; gap: 4px; }.exact-rules b { font-size: 12px; }.exact-rules small { color: var(--ink-500); font-size: 10px; line-height: 1.5; }
.duration-field { display: flex; align-items: center; gap: 10px; }.duration-field span { color: var(--ink-500); font-size: 12px; }
.answer-surface { border-top: 3px solid var(--cinnabar-500); }
.full-width { grid-column: 1 / -1; }
.schedule-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 24px; }
.policy-note { padding: 15px 17px; display: flex; gap: 15px; color: var(--ink-500); background: var(--paper-100); border-radius: 11px; font-size: 12px; line-height: 1.6; }
.policy-note b { color: var(--ink-800); white-space: nowrap; }
@media (max-width: 1050px) { .editor-grid { grid-template-columns: 1fr; } .full-width { grid-column: auto; } }
@media (max-width: 650px) { .form-surface { padding: 20px 16px; } .two-cols, .schedule-grid, .exact-rules { grid-template-columns: 1fr; } }
</style>
