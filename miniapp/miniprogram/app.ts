import { resolveRuntimeConfig } from './config/runtime'

const runtime = resolveRuntimeConfig()
const TEST_SESSION_KEY = 'soft-energy-test-session'
const USER_TOKEN_KEY = 'soft-energy-user-token'
const USER_CACHE_KEY = 'soft-energy-current-user'

function restoredUser(): CurrentUser | null {
  if (!wx.getStorageSync<string>(USER_TOKEN_KEY)) return null
  return wx.getStorageSync<CurrentUser>(USER_CACHE_KEY) || null
}

App<IAppOption>({
  globalData: {
    apiBaseUrl: runtime.apiBaseUrl,
    environment: runtime.environment,
    localDevelopment: runtime.localDevelopment,
    runtimeReady: runtime.configured,
    networkConnected: true,
    user: restoredUser(),
    testAdminToken: null,
    testAdminExpiresAt: null,
  },
  onLaunch() {
    if (!runtime.localDevelopment && wx.getStorageSync(TEST_SESSION_KEY)) {
      wx.removeStorageSync(TEST_SESSION_KEY)
      wx.removeStorageSync(USER_TOKEN_KEY)
      wx.removeStorageSync(USER_CACHE_KEY)
      this.globalData.user = null
    }
    wx.onNetworkStatusChange((status) => {
      this.globalData.networkConnected = status.isConnected
    })
  },
})
