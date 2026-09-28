import { profileApi } from '../../services/api'
import { ensureLogin, updateCurrentUser } from '../../services/auth'

Page({
  data: { first: false, loading: true, saving: false, nickname: '', count: 0, error: '' },
  async onLoad(query: Record<string, string | undefined>) {
    const first = query.first === '1'
    this.setData({ first })
    try {
      const user = await ensureLogin()
      const nickname = user.profileComplete ? user.displayName : ''
      this.setData({ nickname, count: nickname.length })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '身份加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  goBack() { if (!this.data.first) wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) {
    const nickname = event.detail.value
    this.setData({ nickname, count: nickname.length, error: '' })
  },
  async save() {
    const nickname = this.data.nickname.trim()
    if (nickname.length < 2 || nickname.length > 16) {
      this.setData({ error: '修习名需为2至16个字' })
      return
    }
    this.setData({ saving: true, error: '' })
    try {
      const profile = await profileApi.update(nickname)
      const current = getApp<IAppOption>().globalData.user
      if (current) updateCurrentUser({ ...current, displayName: profile.nickname, profileComplete: true })
      wx.showToast({ title: '修习名已保存', icon: 'success' })
      if (this.data.first) wx.reLaunch({ url: '/pages/home/index' })
      else wx.navigateBack()
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '保存失败，请稍后重试' })
    } finally {
      this.setData({ saving: false })
    }
  },
})
