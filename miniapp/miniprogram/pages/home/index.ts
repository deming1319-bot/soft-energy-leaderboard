import { activityApi } from '../../services/api'
import { hasActiveSession } from '../../services/auth'
import { complianceDisplay, complianceInfo } from '../../config/compliance'
import { categoryLabels, formatDateTime, remainingText, timeStateLabels } from '../../utils/format'
import { loginPageUrl } from '../../utils/navigation'
import { isLargeReading } from '../../utils/preferences'
import type { ActivitySummary } from '../../types/index'

type ActivityCard = ActivitySummary & { categoryLabel: string; stateLabel: string; timeText: string; remaining: string }

Page({
  data: {
    loading: true,
    error: '',
    activities: [] as ActivityCard[],
    greeting: '静心修习',
    loggedIn: false,
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
      let loggedIn = hasActiveSession()
      let items: ActivitySummary[]
      try {
        items = loggedIn ? await activityApi.list() : await activityApi.publicList()
      } catch (error) {
        if (!loggedIn) throw error
        loggedIn = false
        items = await activityApi.publicList()
      }
      const activities = items.map((item) => ({ ...item, categoryLabel: categoryLabels[item.category], stateLabel: timeStateLabels[item.timeState], timeText: `${formatDateTime(item.startAt)} — ${formatDateTime(item.endAt)}`, remaining: item.timeState === 'ACTIVE' ? remainingText(item.endAt) : '' }))
      const user = getApp<IAppOption>().globalData.user
      const greeting = loggedIn && user && user.displayName !== '修习者' ? `${user.displayName}，安好` : '今日静心修习'
      this.setData({ activities, loggedIn, activeCount: activities.filter((x) => x.timeState === 'ACTIVE').length, greeting })
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '题目加载失败' }) } finally { this.setData({ loading: false }) }
  },
  retry() { void this.loadData() },
  login() { wx.navigateTo({ url: loginPageUrl('/pages/home/index') }) },
  openActivity(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/activity/index?id=${event.currentTarget.dataset.id}` }) },
  openLeaderboard(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/leaderboard/index?id=${event.currentTarget.dataset.id}` }) },
  copyFilingInfo() { wx.setClipboardData({ data: complianceInfo.filingQueryUrl }) },
})
