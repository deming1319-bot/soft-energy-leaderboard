export type MiniappEnvironment = 'develop' | 'trial' | 'release'

const DEVELOPMENT_API_BASE_URL = 'http://127.0.0.1:8081/api/v1'
const PRODUCTION_API_BASE_URL = 'https://api.ruannengliang.top/api/v1'

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

export function resolveRuntimeConfig(): RuntimeConfig {
  let environment: MiniappEnvironment = 'develop'
  try {
    const value = wx.getAccountInfoSync().miniProgram.envVersion
    if (value === 'trial' || value === 'release') environment = value
  } catch { /* 游客 AppID 与单元测试使用开发环境 */ }

  // 待审开发包在真实审核设备上也可能返回 develop；只有开发者工具模拟器允许访问本机 API。
  const localDevelopment = environment === 'develop' && isDevTools()
  const apiBaseUrl = localDevelopment ? DEVELOPMENT_API_BASE_URL : PRODUCTION_API_BASE_URL
  return {
    environment,
    apiBaseUrl,
    configured: !apiBaseUrl.includes('example.com'),
    localDevelopment,
  }
}
