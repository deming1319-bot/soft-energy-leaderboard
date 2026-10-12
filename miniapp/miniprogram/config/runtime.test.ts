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
    })
  })
})
