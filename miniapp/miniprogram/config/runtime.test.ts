import { afterEach, describe, expect, it, vi } from 'vitest'
import { resolveRuntimeConfig } from './runtime'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('resolveRuntimeConfig', () => {
  it.each(['trial', 'release'] as const)('%s 环境使用已备案的正式 HTTPS API', (envVersion) => {
    vi.stubGlobal('wx', {
      getAccountInfoSync: () => ({
        miniProgram: { envVersion },
      }),
    })

    expect(resolveRuntimeConfig()).toEqual({
      environment: envVersion,
      apiBaseUrl: 'https://api.ruannengliang.top/api/v1',
      configured: true,
      localDevelopment: false,
    })
  })

  it('开发者工具默认使用正式 HTTPS API', () => {
    vi.stubGlobal('wx', {
      getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
      getDeviceInfo: () => ({ platform: 'devtools' }),
      getStorageSync: () => undefined,
    })

    expect(resolveRuntimeConfig()).toEqual({
      environment: 'develop',
      apiBaseUrl: 'https://api.ruannengliang.top/api/v1',
      configured: true,
      localDevelopment: false,
    })
  })

  it('开发者工具明确开启本地调试后才使用本机 API', () => {
    vi.stubGlobal('wx', {
      getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
      getDeviceInfo: () => ({ platform: 'devtools' }),
      getStorageSync: (key: string) => key === 'soft-energy-use-local-api',
    })

    expect(resolveRuntimeConfig()).toEqual({
      environment: 'develop',
      apiBaseUrl: 'http://127.0.0.1:8081/api/v1',
      configured: true,
      localDevelopment: true,
    })
  })

  it('真实设备上的开发或待审版本使用正式 HTTPS API', () => {
    vi.stubGlobal('wx', {
      getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
      getDeviceInfo: () => ({ platform: 'ios' }),
    })

    expect(resolveRuntimeConfig()).toEqual({
      environment: 'develop',
      apiBaseUrl: 'https://api.ruannengliang.top/api/v1',
      configured: true,
      localDevelopment: false,
    })
  })
})
