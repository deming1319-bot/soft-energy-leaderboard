import { reportApi } from '../../services/api'
import { ensureLogin } from '../../services/auth'
import { formatDateTime } from '../../utils/format'
import type { CreateReportPayload, ReportItem, ReportStatus, ReportType } from '../../types/index'

const REPORT_TYPES: Array<{ value: ReportType; label: string; tip: string }> = [
  { value: 'QUESTION_CONTENT', label: '题目内容', tip: '内容错误、不当或需要核实' },
  { value: 'INAPPROPRIATE_NICKNAME', label: '昵称不当', tip: '榜单昵称含违规或冒犯内容' },
  { value: 'SCORE_APPEAL', label: '评分申诉', tip: '对自动判定或复核结果有疑问' },
  { value: 'INFRINGEMENT', label: '侵权投诉', tip: '著作权、名誉权等权益问题' },
  { value: 'ILLEGAL_CONTENT', label: '违法不良信息', tip: '涉嫌违法或危害公共安全' },
  { value: 'PRIVACY', label: '隐私问题', tip: '个人信息查询、更正或删除请求' },
  { value: 'OTHER', label: '其他反馈', tip: '功能异常或改进建议' },
]
const TYPE_LABELS = Object.fromEntries(REPORT_TYPES.map((item) => [item.value, item.label])) as Record<ReportType, string>
const STATUS_LABELS: Record<ReportStatus, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已办结', REJECTED: '已驳回' }

Page({
  data: {
    loading: true,
    submitting: false,
    types: REPORT_TYPES,
    selectedType: 'OTHER' as ReportType,
    fixedType: false,
    activityId: '',
    targetUserId: '',
    submissionId: '',
    targetLabel: '平台服务',
    description: '',
    count: 0,
    error: '',
    reports: [] as Array<ReportItem & { typeLabel: string; statusLabel: string; timeText: string }>,
  },
  async onLoad(query: Record<string, string | undefined>) {
    const requested = query.type as ReportType | undefined
    const selectedType = REPORT_TYPES.some((item) => item.value === requested) ? requested! : 'OTHER'
    this.setData({
      selectedType,
      fixedType: Boolean(requested),
      activityId: query.activityId || '',
      targetUserId: query.targetUserId || '',
      submissionId: query.submissionId || '',
      targetLabel: query.targetLabel ? decodeURIComponent(query.targetLabel) : '平台服务',
    })
    await this.loadReports()
  },
  async onPullDownRefresh() { await this.loadReports(); wx.stopPullDownRefresh() },
  goBack() { wx.navigateBack() },
  selectType(event: WechatMiniprogram.TouchEvent) {
    if (this.data.fixedType) return
    this.setData({ selectedType: event.currentTarget.dataset.value as ReportType, error: '' })
  },
  onInput(event: WechatMiniprogram.Input) {
    const description = event.detail.value
    this.setData({ description, count: description.length, error: '' })
  },
  async loadReports() {
    try {
      await ensureLogin()
      const reports = await reportApi.mine()
      this.setData({ reports: reports.slice(0, 10).map((item) => ({
        ...item,
        typeLabel: TYPE_LABELS[item.type],
        statusLabel: STATUS_LABELS[item.status],
        timeText: formatDateTime(item.createdAt),
      })) })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '反馈记录加载失败' })
    } finally { this.setData({ loading: false }) }
  },
  async submit() {
    const description = this.data.description.trim()
    if (description.length < 5) {
      this.setData({ error: '请至少用5个字说明具体情况' })
      return
    }
    this.setData({ submitting: true, error: '' })
    const payload: CreateReportPayload = { type: this.data.selectedType, description }
    if (this.data.activityId) payload.activityId = this.data.activityId
    if (this.data.targetUserId) payload.targetUserId = this.data.targetUserId
    if (this.data.submissionId) payload.submissionId = this.data.submissionId
    try {
      await reportApi.create(payload)
      this.setData({ description: '', count: 0 })
      wx.showToast({ title: '反馈已提交', icon: 'success' })
      await this.loadReports()
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '提交失败，请稍后重试' })
    } finally { this.setData({ submitting: false }) }
  },
})
