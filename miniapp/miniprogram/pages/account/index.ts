import { profileApi } from '../../services/api'
import { clearLoginSession, ensureLogin } from '../../services/auth'
import type { Profile } from '../../types/index'

Page({
  data: {
    loading: true,
    cancelling: false,
    profile: null as Profile | null,
    initial: '修',
    honorCount: 0,
    confirmText: '',
    error: '',
  },
  async onLoad() {
    try {
      await ensureLogin()
      const profile = await profileApi.get()
      this.setData({
        profile,
        initial: profile.nickname.slice(0, 1) || '修',
        honorCount: profile.championCount + profile.runnerUpCount,
      })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '账号信息加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  goBack() { wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) { this.setData({ confirmText: event.detail.value, error: '' }) },
  async cancelAccount() {
    if (this.data.confirmText.trim() !== '确认注销') {
      this.setData({ error: '请输入“确认注销”后再继续' })
      return
    }
    const confirmation = await wx.showModal({
      title: '最后确认账号注销',
      content: '注销后原账号不能恢复，身份、原始答案和申诉内容会被清除；榜单只保留无法关联到个人的匿名荣誉记录。',
      confirmText: '确认注销',
      confirmColor: '#9a3f35',
    })
    if (!confirmation.confirm) return
    this.setData({ cancelling: true, error: '' })
    try {
      await profileApi.cancel()
      clearLoginSession(true)
      wx.showToast({ title: '账号已注销', icon: 'success', duration: 1200 })
      setTimeout(() => wx.reLaunch({ url: '/pages/welcome/index' }), 500)
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '注销失败，请稍后重试' })
    } finally {
      this.setData({ cancelling: false })
    }
  },
})
