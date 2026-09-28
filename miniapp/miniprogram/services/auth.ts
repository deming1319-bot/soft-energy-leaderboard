import { request } from './request'
import { PRIVACY_VERSION, TERMS_VERSION } from '../config/legal'
import type { ActiveTestIdentity, TokenResponse } from '../types/index'

export const TOKEN_KEY = 'soft-energy-user-token'
const DEVICE_KEY = 'soft-energy-device-id'
const CONSENT_KEY = 'soft-energy-legal-consent'
export const TEST_SESSION_KEY = 'soft-energy-test-session'
let loginTask: Promise<CurrentUser> | null = null

interface LegalConsent {
  privacyVersion: string
  termsVersion: string
  acceptedAt: string
}

function deviceId(): string {
  let value = wx.getStorageSync<string>(DEVICE_KEY)
  if (!value) { value = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 18)}`; wx.setStorageSync(DEVICE_KEY, value) }
  return value
}

export function ensureLogin(force = false): Promise<CurrentUser> {
  const app = getApp<IAppOption>()
  const consent = currentConsent()
  if (!consent) return Promise.reject(new Error('请先阅读并同意用户协议与隐私保护指引'))
  if (!app.globalData.runtimeReady) return Promise.reject(new Error('正式服务地址尚未配置'))
  if (!force && app.globalData.user && wx.getStorageSync(TOKEN_KEY)) return Promise.resolve(app.globalData.user)
  if (!app.globalData.user && currentTestIdentity()) {
    wx.removeStorageSync(TEST_SESSION_KEY)
    wx.removeStorageSync(TOKEN_KEY)
  }
  if (loginTask) return loginTask
  loginTask = new Promise((resolve, reject) => {
    wx.login({
      async success(result) {
        try {
          const loginData: Record<string, unknown> = {
            code: result.code || 'dev-code',
            agreementAccepted: true,
            privacyVersion: consent.privacyVersion,
            termsVersion: consent.termsVersion,
          }
          if (app.globalData.environment === 'develop') loginData.deviceId = deviceId()
          const response = await request<{ accessToken: string; currentUser: CurrentUser }>({
            url: '/miniapp/auth/login',
            method: 'POST',
            data: loginData,
          })
          wx.setStorageSync(TOKEN_KEY, response.accessToken)
          app.globalData.user = response.currentUser
          resolve(response.currentUser)
        } catch (error) { reject(error) } finally { loginTask = null }
      },
      fail(error) { loginTask = null; reject(new Error(error.errMsg)) },
    })
  })
  return loginTask
}

export function acceptCurrentAgreements(): LegalConsent {
  const consent = {
    privacyVersion: PRIVACY_VERSION,
    termsVersion: TERMS_VERSION,
    acceptedAt: new Date().toISOString(),
  }
  wx.setStorageSync(CONSENT_KEY, consent)
  return consent
}

export function currentConsent(): LegalConsent | null {
  const consent = wx.getStorageSync<LegalConsent>(CONSENT_KEY)
  if (!consent || consent.privacyVersion !== PRIVACY_VERSION || consent.termsVersion !== TERMS_VERSION) return null
  return consent
}

export function clearLoginSession(clearConsent = false): void {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(TEST_SESSION_KEY)
  if (clearConsent) wx.removeStorageSync(CONSENT_KEY)
  getApp<IAppOption>().globalData.user = null
}

export function updateCurrentUser(user: CurrentUser): void {
  getApp<IAppOption>().globalData.user = user
}

export function currentTestIdentity(): ActiveTestIdentity | null {
  const identity = wx.getStorageSync<ActiveTestIdentity>(TEST_SESSION_KEY)
  if (!identity || typeof identity.testerKey !== 'string' || typeof identity.nickname !== 'string' || !identity.currentUser) return null
  return identity
}

export function activateTestSession(response: TokenResponse, account: Pick<ActiveTestIdentity, 'testerKey' | 'nickname'>): void {
  const app = getApp<IAppOption>()
  const identity: ActiveTestIdentity = { ...account, currentUser: response.currentUser }
  wx.setStorageSync(TOKEN_KEY, response.accessToken)
  wx.setStorageSync(TEST_SESSION_KEY, identity)
  app.globalData.user = response.currentUser
}

export async function exitTestSession(): Promise<CurrentUser> {
  const app = getApp<IAppOption>()
  wx.removeStorageSync(TEST_SESSION_KEY)
  wx.removeStorageSync(TOKEN_KEY)
  app.globalData.user = null
  return ensureLogin(true)
}
