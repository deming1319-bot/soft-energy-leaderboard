import { activityApi } from '../../services/api'
import { ensureLogin } from '../../services/auth'
import { formatDateTime, gradeLabels } from '../../utils/format'
import { isLargeReading } from '../../utils/preferences'
import type { SubmissionResult } from '../../types/index'

Page({
  data: {
    id: '',
    fresh: false,
    largeReading: false,
    loading: true,
    error: '',
    result: null as (SubmissionResult & { gradeLabel: string; timeText: string; scorePercent: number }) | null,
  },
  async onLoad(query: Record<string, string | undefined>) {
    if (!query.id) {
      this.setData({ loading: false, error: '缺少答题记录，请返回首页重新进入' })
      return
    }
    this.setData({ id: query.id, fresh: query.fresh === '1', largeReading: isLargeReading() })
    await this.loadData()
  },
  onShow() { this.setData({ largeReading: isLargeReading() }) },
  goBack() { wx.reLaunch({ url: '/pages/home/index' }) },
  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      await ensureLogin()
      const cached = this.data.fresh ? wx.getStorageSync<SubmissionResult>(`submission-result:${this.data.id}`) : null
      const result = cached || await activityApi.mySubmission(this.data.id)
      this.setData({
        result: {
          ...result,
          gradeLabel: gradeLabels[result.grade],
          timeText: formatDateTime(result.submittedAt),
          scorePercent: Math.round(Number(result.score) * 100),
        },
        fresh: false,
      })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '结果加载失败，请检查网络后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },
  retry() { void this.loadData() },
  viewLeaderboard() { wx.navigateTo({ url: `/pages/leaderboard/index?id=${this.data.id}` }) },
  backHome() { wx.reLaunch({ url: '/pages/home/index' }) },
  appealScore() {
    if (!this.data.result) return
    const label = encodeURIComponent(this.data.result.activityTitle)
    wx.navigateTo({ url: `/pages/report/index?type=SCORE_APPEAL&submissionId=${this.data.result.id}&targetLabel=${label}` })
  },
})
