import { profileApi } from '../../services/api'
import { currentTestIdentity, ensureLogin } from '../../services/auth'
import { formatDateTime, gradeLabels } from '../../utils/format'
import { isLargeReading, setLargeReading } from '../../utils/preferences'
import type { Profile } from '../../types/index'

Page({
  data: { loading: true, profile: null as Profile | null, results: [] as Array<Record<string, unknown>>, initial: '修', largeReading: false, showTestCenter: false, testIdentityName: '' },
  async onLoad() { this.refreshTestState(); this.setData({ largeReading: isLargeReading() }); await this.loadData() },
  async onShow() { const tab = this.getTabBar?.(); if (tab) tab.setData({ selected: 1 }); this.refreshTestState(); this.setData({ largeReading: isLargeReading() }); if (!this.data.loading) await this.loadData() },
  async onPullDownRefresh() { await this.loadData(); wx.stopPullDownRefresh() },
  async loadData() {
    try { await ensureLogin(); const profile = await profileApi.get(); const results = profile.recentResults.map(x => ({ ...x, gradeLabel: gradeLabels[x.grade], timeText: formatDateTime(x.submittedAt), initial: x.activityTitle.slice(0,1) })); this.setData({ profile, results, initial: profile.nickname.slice(0,1) || '修' }) }
    catch (error) { wx.showToast({ title: error instanceof Error ? error.message : '档案加载失败', icon: 'none' }) } finally { this.setData({ loading: false }) }
  },
  openResult(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/result/index?id=${event.currentTarget.dataset.id}` }) },
  openPrivacy() { wx.navigateTo({ url: '/pages/privacy/index' }) },
  editProfile() { wx.navigateTo({ url: '/pages/profile-edit/index' }) },
  openTerms() { wx.navigateTo({ url: '/pages/legal/index?type=terms' }) },
  openAccount() { wx.navigateTo({ url: '/pages/account/index' }) },
  openReport() { wx.navigateTo({ url: '/pages/report/index' }) },
  openTestCenter() { wx.navigateTo({ url: '/pages/test-center/index' }) },
  refreshTestState() {
    const identity = currentTestIdentity()
    this.setData({
      showTestCenter: getApp<IAppOption>().globalData.environment === 'develop',
      testIdentityName: identity?.nickname || '',
    })
  },
  switchReading(event: WechatMiniprogram.SwitchChange) {
    const enabled = event.detail.value
    setLargeReading(enabled)
    this.setData({ largeReading: enabled })
    wx.showToast({ title: enabled ? '已开启大字阅读' : '已使用标准字号', icon: 'none' })
  },
})
