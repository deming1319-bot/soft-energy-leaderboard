import { activateTestSession, currentTestIdentity, exitTestSession } from '../../services/auth'
import { testCenterApi } from '../../services/testCenter'
import type { TestAccount } from '../../types/index'

function adminSessionIsValid(app: IAppOption): boolean {
  if (!app.globalData.testAdminToken || !app.globalData.testAdminExpiresAt) return false
  return Date.parse(app.globalData.testAdminExpiresAt) > Date.now() + 30_000
}

Page({
  data: {
    available: true,
    unlocked: false,
    loading: false,
    switchingKey: '',
    username: 'admin',
    password: '',
    accounts: [] as TestAccount[],
    currentTesterKey: '',
    currentNickname: '',
    error: '',
  },
  async onLoad() {
    const app = getApp<IAppOption>()
    if (app.globalData.environment !== 'develop') {
      this.setData({ available: false })
      return
    }
    this.refreshCurrentIdentity()
    if (adminSessionIsValid(app)) {
      this.setData({ unlocked: true })
      await this.loadAccounts()
    }
  },
  goBack() { wx.navigateBack() },
  inputUsername(event: WechatMiniprogram.Input) { this.setData({ username: event.detail.value, error: '' }) },
  inputPassword(event: WechatMiniprogram.Input) { this.setData({ password: event.detail.value, error: '' }) },
  refreshCurrentIdentity() {
    const identity = currentTestIdentity()
    this.setData({
      currentTesterKey: identity?.testerKey || '',
      currentNickname: identity?.nickname || '',
    })
  },
  async unlock() {
    const username = this.data.username.trim()
    const password = this.data.password
    if (!username || !password) {
      this.setData({ error: '请输入管理员账号和密码' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const response = await testCenterApi.loginAdmin(username, password)
      if (response.currentUser.role !== 'ADMIN') throw new Error('该账号没有管理员权限')
      const app = getApp<IAppOption>()
      app.globalData.testAdminToken = response.accessToken
      app.globalData.testAdminExpiresAt = response.expiresAt
      this.setData({ unlocked: true, password: '' })
      await this.loadAccounts()
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '管理员验证失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  async loadAccounts() {
    const app = getApp<IAppOption>()
    if (!adminSessionIsValid(app)) {
      this.lockAdminSession('管理员验证已过期，请重新验证')
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const accounts = await testCenterApi.accounts(app.globalData.testAdminToken as string)
      this.setData({ accounts })
    } catch (error) {
      this.lockAdminSession(error instanceof Error ? error.message : '测试账号加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },
  lockAdminSession(error = '') {
    const app = getApp<IAppOption>()
    app.globalData.testAdminToken = null
    app.globalData.testAdminExpiresAt = null
    this.setData({ unlocked: false, accounts: [], password: '', error })
  },
  reverifyAdmin() { this.lockAdminSession() },
  async selectAccount(event: WechatMiniprogram.TouchEvent) {
    const testerKey = String(event.currentTarget.dataset.key || '')
    const account = this.data.accounts.find((item) => item.testerKey === testerKey)
    if (!account || this.data.switchingKey) return
    const result = await wx.showModal({
      title: `切换为${account.nickname}`,
      content: '切换后，首页、答题、成绩和个人中心都会使用该测试学员的数据，并与管理系统实时同步。',
      confirmText: '确认切换',
      confirmColor: '#465e50',
    })
    if (!result.confirm) return
    const app = getApp<IAppOption>()
    if (!adminSessionIsValid(app)) {
      this.lockAdminSession('管理员验证已过期，请重新验证')
      return
    }
    this.setData({ switchingKey: account.testerKey, error: '' })
    try {
      const response = await testCenterApi.startSession(app.globalData.testAdminToken as string, account)
      activateTestSession(response, account)
      this.refreshCurrentIdentity()
      wx.showToast({ title: `已切换为${account.nickname}`, icon: 'success' })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '测试账号切换失败' })
    } finally {
      this.setData({ switchingKey: '' })
    }
  },
  openQuestions() {
    if (!this.data.currentTesterKey) {
      wx.showToast({ title: '请先选择测试账号', icon: 'none' })
      return
    }
    wx.switchTab({ url: '/pages/home/index' })
  },
  async exitIdentity() {
    const result = await wx.showModal({
      title: '退出测试身份',
      content: '将恢复当前微信开发者的普通测试身份，已提交的测试答题数据不会删除。',
      confirmText: '确认退出',
      confirmColor: '#9d493d',
    })
    if (!result.confirm) return
    this.setData({ loading: true, error: '' })
    try {
      await exitTestSession()
      this.refreshCurrentIdentity()
      wx.showToast({ title: '已退出测试身份', icon: 'success' })
    } catch (error) {
      this.setData({ error: error instanceof Error ? error.message : '普通身份恢复失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
