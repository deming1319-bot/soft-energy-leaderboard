import { acceptCurrentAgreements, currentConsent, ensureLogin } from '../../services/auth'

Page({
  data: {
    checked: false,
    entering: false,
    error: '',
    privacyName: '《问道修习用户隐私保护指引》',
    platformAuthorizationRequired: false,
    runtimeReady: true,
  },
  onLoad() {
    const app = getApp<IAppOption>()
    this.setData({ runtimeReady: app.globalData.runtimeReady })
    if (typeof wx.getPrivacySetting === 'function') {
      wx.getPrivacySetting({
        success: (result) => {
          this.setData({
            checked: Boolean(currentConsent()),
            privacyName: result.privacyContractName || this.data.privacyName,
            platformAuthorizationRequired: result.needAuthorization,
          })
          if (currentConsent() && !result.needAuthorization) void this.enter(false)
        },
        fail: () => {
          if (currentConsent()) void this.enter(false)
        },
      })
      return
    }
    if (currentConsent()) void this.enter(false)
  },
  toggleAgreement() {
    if (this.data.entering) return
    this.setData({ checked: !this.data.checked, error: '' })
  },
  openPrivacy() {
    if (typeof wx.openPrivacyContract !== 'function') {
      wx.navigateTo({ url: '/pages/legal/index?type=privacy' })
      return
    }
    wx.openPrivacyContract({
      fail: () => wx.navigateTo({ url: '/pages/legal/index?type=privacy' }),
    })
  },
  openTerms() {
    wx.navigateTo({ url: '/pages/legal/index?type=terms' })
  },
  handlePrivacyAgreement() {
    if (!this.data.checked) return
    void this.enter(true)
  },
  handleButtonTap() {
    if (!this.data.checked || this.data.entering) return
    // 未配置平台隐私指引的游客 AppID 不一定触发 agreePrivacyAuthorization 事件，开发环境使用点击回退。
    if (!this.data.platformAuthorizationRequired || getApp<IAppOption>().globalData.environment === 'develop') void this.enter(true)
  },
  async enter(saveAgreement: boolean) {
    if (this.data.entering) return
    if (!this.data.runtimeReady) {
      this.setData({ error: '正式服务地址尚未配置，暂时不能进入。' })
      return
    }
    this.setData({ entering: true, error: '' })
    try {
      if (saveAgreement) acceptCurrentAgreements()
      const user = await ensureLogin(true)
      const target = user.profileComplete ? '/pages/home/index' : '/pages/profile-edit/index?first=1'
      wx.reLaunch({ url: target })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '暂时无法进入，请稍后重试', entering: false })
    }
  },
})
