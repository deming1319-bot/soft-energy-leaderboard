import { activityApi } from '../../services/api'
import { ensureLogin } from '../../services/auth'
import { formatDateTime, gradeLabels } from '../../utils/format'
import { isLargeReading } from '../../utils/preferences'
import type { Leaderboard, LeaderboardEntry } from '../../types/index'

type RankEntry = LeaderboardEntry & { timeText: string; initial: string; reportable: boolean }
Page({
  data: { id: '', loading: true, error: '', largeReading: false, board: null as Leaderboard | null, champions: [] as RankEntry[], runners: [] as RankEntry[], endText: '', myGrade: '' },
  async onLoad(query: Record<string,string|undefined>) {
    if (!query.id) {
      this.setData({ loading: false, error: '缺少题目信息，请返回首页重新进入' })
      return
    }
    this.setData({ id: query.id, largeReading: isLargeReading() })
    await this.loadData()
  },
  onShow() { this.setData({ largeReading: isLargeReading() }) },
  async onPullDownRefresh() { await this.loadData(); wx.stopPullDownRefresh() },
  goBack() { wx.navigateBack() },
  async loadData() {
    this.setData({ loading: true, error: '' })
    try { await ensureLogin(); const board = await activityApi.leaderboard(this.data.id); const adapt = (items: LeaderboardEntry[]) => items.map(x => ({ ...x, timeText: formatDateTime(x.submittedAt), initial: x.displayName.slice(0,1), reportable: !x.currentUser && !x.userId.startsWith('anonymous:') })); this.setData({ board, champions: adapt(board.champions), runners: adapt(board.runnersUp), endText: formatDateTime(board.endAt), myGrade: board.myResult ? gradeLabels[board.myResult.grade] : '' }) }
    catch (error) { this.setData({ error: error instanceof Error ? error.message : '榜单加载失败，请检查网络后重试' }) } finally { this.setData({ loading: false }) }
  },
  retry() { void this.loadData() },
  backHome() { wx.reLaunch({ url: '/pages/home/index' }) },
  reportUser(event: WechatMiniprogram.TouchEvent) {
    const userId = event.currentTarget.dataset.userId as string
    const label = encodeURIComponent(event.currentTarget.dataset.label as string)
    wx.navigateTo({ url: `/pages/report/index?type=INAPPROPRIATE_NICKNAME&targetUserId=${userId}&targetLabel=${label}` })
  },
})
