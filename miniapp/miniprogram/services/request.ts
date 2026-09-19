import type { ApiResponse } from '../types/index'

const TOKEN_KEY = 'soft-energy-user-token'
const TEST_SESSION_KEY = 'soft-energy-test-session'

export function request<T>(options: WechatMiniprogram.RequestOption): Promise<T> {
  const app = getApp<IAppOption>()
  const token = wx.getStorageSync<string>(TOKEN_KEY)
  return new Promise((resolve, reject) => {
    wx.request<ApiResponse<T>>({
      ...options,
      url: `${app.globalData.apiBaseUrl}${options.url}`,
      timeout: options.timeout || 12000,
      header: { 'content-type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.header },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) { resolve(response.data.data); return }
        if (response.statusCode === 401) {
          wx.removeStorageSync(TOKEN_KEY)
          wx.removeStorageSync(TEST_SESSION_KEY)
          app.globalData.user = null
        }
        reject(new Error(response.data?.message || `请求失败（${response.statusCode}）`))
      },
      fail: (error) => {
        const message = error.errMsg?.includes('timeout')
          ? '网络响应超时，请检查网络后重试'
          : '网络连接失败，请检查网络后重试'
        reject(new Error(message))
      },
    })
  })
}

export function newRequestId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}
