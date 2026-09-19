import { resolveRuntimeConfig } from './config/runtime'

const runtime = resolveRuntimeConfig()
const TEST_SESSION_KEY = 'soft-energy-test-session'
const USER_TOKEN_KEY = 'soft-energy-user-token'

function restoredTestUser(): CurrentUser | null {
  const session = wx.getStorageSync<{ currentUser?: CurrentUser }>(TEST_SESSION_KEY)
  if (!session?.currentUser || !wx.getStorageSync<string>(USER_TOKEN_KEY)) return null
  return session.currentUser
}

App<IAppOption>({
  globalData: {
    apiBaseUrl: runtime.apiBaseUrl,
    environment: runtime.environment,
    runtimeReady: runtime.configured,
    networkConnected: true,
    user: runtime.environment === 'develop' ? restoredTestUser() : null,
    testAdminToken: null,
    testAdminExpiresAt: null,
  },
  onLaunch() {
    if (runtime.environment !== 'develop' && wx.getStorageSync(TEST_SESSION_KEY)) {
      wx.removeStorageSync(TEST_SESSION_KEY)
      wx.removeStorageSync(USER_TOKEN_KEY)
    }
    wx.onNetworkStatusChange((status) => {
      this.globalData.networkConnected = status.isConnected
    })
  },
})
