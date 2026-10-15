import { describe, expect, it } from 'vitest'
import { loginPageUrl, safePagePath } from './navigation'

describe('游客登录跳转', () => {
  it('保留题目详情作为登录后的返回目标', () => {
    const target = '/pages/activity/index?id=activity-1'

    expect(safePagePath(encodeURIComponent(target))).toBe(target)
    expect(loginPageUrl(target)).toBe(`/pages/welcome/index?redirect=${encodeURIComponent(target)}`)
  })

  it('拒绝外部地址并回退首页', () => {
    expect(safePagePath(encodeURIComponent('https://example.com'))).toBe('/pages/home/index')
    expect(safePagePath('%E0%A4%A')).toBe('/pages/home/index')
  })
})
