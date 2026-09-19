import { describe, expect, it } from 'vitest'
import { remainingText } from './format'

describe('remainingText', () => {
  it('将剩余时间转为易读文案', () => {
    expect(remainingText('2026-07-26T10:31:00Z', new Date('2026-07-26T08:00:00Z').getTime())).toBe('余 2 小时 31 分')
  })
  it('截止后返回明确状态', () => {
    expect(remainingText('2026-07-26T08:00:00Z', new Date('2026-07-26T09:00:00Z').getTime())).toBe('已截止')
  })
})
