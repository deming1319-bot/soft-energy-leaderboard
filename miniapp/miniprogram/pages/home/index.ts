import { activityApi } from '../../services/api'
import { ensureLogin } from '../../services/auth'
import { complianceDisplay, complianceInfo } from '../../config/compliance'
import { categoryLabels, formatDateTime, remainingText, timeStateLabels } from '../../utils/format'
import { isLargeReading } from '../../utils/preferences'
import type { ActivitySummary } from '../../types/index'

type ActivityCard = ActivitySummary & { categoryLabel: string; stateLabel: string; timeText: string; remaining: string }

Page({
  data: {
    loading: true,
    error: '',
    activities: [] as ActivityCard[],
    greeting: '静心修习',
    activeCount: 0,
    largeReading: false,
    operatorName: complianceDisplay(complianceInfo.operatorName),
    filingNumber: complianceDisplay(complianceInfo.appFilingNumber),
  },
  async onLoad() { this.setData({ largeReading: isLargeReading() }); await this.loadData() },
  async onShow() { const tab = this.getTabBar?.(); if (tab) tab.setData({ selected: 0 }); this.setData({ largeReading: isLargeReading() }) },
  async onPullDownRefresh() { await this.loadData(); wx.stopPullDownRefresh() },
  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const user = await ensureLogin()
      if (!user.profileComplete) {
        wx.navigateTo({ url: '/pages/profile-edit/index?first=1' })
        return
      }
      const items = await activityApi.list()
      const activities = items.map((item) => ({ ...item, categoryLabel: categoryLabels[item.category], stateLabel: timeStateLabels[item.timeState], timeText: `${formatDateTime(item.startAt)} — ${formatDateTime(item.endAt)}`, remaining: item.timeState === 'ACTIVE' ? remainingText(item.endAt) : '' }))
      this.setData({ activities, activeCount: activities.filter((x) => x.timeState === 'ACTIVE').length, greeting: user.displayName === '修习者' ? '今日静心修习' : `${user.displayName}，安好` })
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '题目加载失败' }) } finally { this.setData({ loading: false }) }
  },
  retry() { void this.loadData() },
  openActivity(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/activity/index?id=${event.currentTarget.dataset.id}` }) },
  openLeaderboard(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/leaderboard/index?id=${event.currentTarget.dataset.id}` }) },
  copyFilingInfo() { wx.setClipboardData({ data: complianceInfo.filingQueryUrl }) },
})
