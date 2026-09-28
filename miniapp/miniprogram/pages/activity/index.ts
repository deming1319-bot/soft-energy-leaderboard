import { activityApi } from '../../services/api'
import { ensureLogin } from '../../services/auth'
import { newRequestId } from '../../services/request'
import { categoryLabels, formatDateTime, remainingText, timeStateLabels } from '../../utils/format'
import { isLargeReading } from '../../utils/preferences'
import type { ActivityDetail } from '../../types/index'

interface PendingSubmission { requestId: string; answer: string }

Page({
  data: { id: '', loading: true, submitting: false, error: '', submitError: '', largeReading: false, detail: null as (ActivityDetail & { categoryLabel: string; stateLabel: string; timeText: string; remaining: string }) | null, answer: '', count: 0 },
  async onLoad(query: Record<string, string | undefined>) {
    if (!query.id) { wx.navigateBack(); return }
    const draft = wx.getStorageSync<string>(`answer-draft:${query.id}`) || ''
    this.setData({ id: query.id, answer: draft, count: draft.length, largeReading: isLargeReading() })
    await this.loadData()
  },
  goBack() { wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) { const answer = event.detail.value; this.setData({ answer, count: answer.length, submitError: '' }); wx.setStorageSync(`answer-draft:${this.data.id}`, answer) },
  async loadData() {
    this.setData({ loading: true, error: '' })
    try { const user = await ensureLogin(); if (!user.profileComplete) { wx.navigateTo({ url: '/pages/profile-edit/index?first=1' }); return }; const detail = await activityApi.detail(this.data.id); this.setData({ detail: { ...detail, categoryLabel: categoryLabels[detail.category], stateLabel: timeStateLabels[detail.timeState], timeText: `${formatDateTime(detail.startAt)} 至 ${formatDateTime(detail.endAt)}`, remaining: detail.timeState === 'ACTIVE' ? remainingText(detail.endAt) : '' } }); if (detail.submitted && !detail.canSubmit) wx.setNavigationBarTitle({ title: '答题结果' }) }
    catch (error) { this.setData({ error: error instanceof Error ? error.message : '题目加载失败' }) } finally { this.setData({ loading: false }) }
  },
  retry() { void this.loadData() },
  viewResult() { wx.navigateTo({ url: `/pages/result/index?id=${this.data.id}` }) },
  viewLeaderboard() { wx.navigateTo({ url: `/pages/leaderboard/index?id=${this.data.id}` }) },
  reportQuestion() {
    const label = encodeURIComponent(this.data.detail?.title || '当前题目')
    wx.navigateTo({ url: `/pages/report/index?type=QUESTION_CONTENT&activityId=${this.data.id}&targetLabel=${label}` })
  },
  async submit() {
    const answer = this.data.answer.trim()
    if (answer.length < 2) { wx.showToast({ title: '请先写下你的答案', icon: 'none' }); return }
    const confirm = await wx.showModal({ title: '确认提交答案', content: '提交后系统将立即判题。请再次确认内容完整，顺序无误。', confirmText: '确认提交', confirmColor: '#9d493d' })
    if (!confirm.confirm) return
    this.setData({ submitting: true, submitError: '' })
    const pendingKey = `pending-submission:${this.data.id}`
    const pending = wx.getStorageSync<PendingSubmission>(pendingKey)
    const requestId = pending && pending.answer === answer ? pending.requestId : newRequestId()
    wx.setStorageSync(pendingKey, { requestId, answer })
    try { const result = await activityApi.submit(this.data.id, answer, requestId); wx.removeStorageSync(pendingKey); wx.removeStorageSync(`answer-draft:${this.data.id}`); wx.setStorageSync(`submission-result:${this.data.id}`, result); wx.redirectTo({ url: `/pages/result/index?id=${this.data.id}&fresh=1` }) }
    catch (error) { this.setData({ submitError: error instanceof Error ? error.message : '提交失败，答案已经保留' }) } finally { this.setData({ submitting: false }) }
  },
})
