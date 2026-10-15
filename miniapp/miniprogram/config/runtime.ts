export type MiniappEnvironment = 'develop' | 'trial' | 'release'

const DEVELOPMENT_API_BASE_URL = 'http://127.0.0.1:8081/api/v1'
const PRODUCTION_API_BASE_URL = 'https://api.ruannengliang.top/api/v1'
const LOCAL_API_OPT_IN_KEY = 'soft-energy-use-local-api'

export interface RuntimeConfig {
  environment: MiniappEnvironment
  apiBaseUrl: string
  configured: boolean
  localDevelopment: boolean
}

function isDevTools(): boolean {
  try {
    return wx.getDeviceInfo().platform === 'devtools'
  } catch {
    return false
  }
}

function isLocalApiEnabled(): boolean {
  try {
    return wx.getStorageSync<boolean>(LOCAL_API_OPT_IN_KEY) === true
  } catch {
    return false
  }
}

export function resolveRuntimeConfig(): RuntimeConfig {
  let environment: MiniappEnvironment = 'develop'
  try {
    const value = wx.getAccountInfoSync().miniProgram.envVersion
    if (value === 'trial' || value === 'release') environment = value
  } catch { /* 游客 AppID 与单元测试使用开发环境 */ }

  // 开发者工具也默认访问线上 HTTPS，避免预览、上传前检查误连未启动的本机服务。
  // 仅开发人员在 Storage 中明确设置 soft-energy-use-local-api=true 时启用本机 API。
  const localDevelopment = environment === 'develop' && isDevTools() && isLocalApiEnabled()
  const apiBaseUrl = localDevelopment ? DEVELOPMENT_API_BASE_URL : PRODUCTION_API_BASE_URL
  return {
    environment,
    apiBaseUrl,
    configured: !apiBaseUrl.includes('example.com'),
    localDevelopment,
  }
}
