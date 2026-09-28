export type MiniappEnvironment = 'develop' | 'trial' | 'release'

const DEVELOPMENT_API_BASE_URL = 'http://127.0.0.1:8081/api/v1'
const PRODUCTION_API_BASE_URL = 'https://api.example.com/api/v1'

export interface RuntimeConfig {
  environment: MiniappEnvironment
  apiBaseUrl: string
  configured: boolean
}

export function resolveRuntimeConfig(): RuntimeConfig {
  let environment: MiniappEnvironment = 'develop'
  try {
    const value = wx.getAccountInfoSync().miniProgram.envVersion
    if (value === 'trial' || value === 'release') environment = value
  } catch { /* 游客 AppID 与单元测试使用开发环境 */ }

  const apiBaseUrl = environment === 'develop' ? DEVELOPMENT_API_BASE_URL : PRODUCTION_API_BASE_URL
  return {
    environment,
    apiBaseUrl,
    configured: environment === 'develop' || !apiBaseUrl.includes('example.com'),
  }
}
