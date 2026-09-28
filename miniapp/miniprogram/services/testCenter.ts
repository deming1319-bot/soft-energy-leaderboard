import type { ApiResponse, TestAccount, TokenResponse } from '../types/index'

type RequestMethod = 'GET' | 'POST'

function adminRequest<T>(url: string, method: RequestMethod, data?: WechatMiniprogram.IAnyObject, token?: string): Promise<T> {
  const app = getApp<IAppOption>()
  return new Promise((resolve, reject) => {
    wx.request<ApiResponse<T>>({
      url: `${app.globalData.apiBaseUrl}${url}`,
      method,
      data,
      timeout: 12000,
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data.data)
          return
        }
        reject(new Error(response.data?.message || `请求失败（${response.statusCode}）`))
      },
      fail(error) {
        reject(new Error(error.errMsg?.includes('timeout') ? '网络响应超时，请稍后重试' : '无法连接服务器，请检查网络'))
      },
    })
  })
}

export const testCenterApi = {
  loginAdmin: (username: string, password: string) =>
    adminRequest<TokenResponse>('/admin/auth/login', 'POST', { username, password }),
  accounts: (adminToken: string) =>
    adminRequest<TestAccount[]>('/admin/test-center/accounts', 'GET', undefined, adminToken),
  startSession: (adminToken: string, account: TestAccount) =>
    adminRequest<TokenResponse>(
      '/admin/test-center/session',
      'POST',
      { nickname: account.nickname, testerKey: account.testerKey },
      adminToken,
    ),
}
