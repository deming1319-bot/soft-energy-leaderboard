import { describe, expect, it } from 'vitest'
import { mergeTestIdentities, type TestIdentity } from './testIdentities'

const defaultIdentities: TestIdentity[] = [
  { testerKey: 'preset-student-01', nickname: '张明' },
  { testerKey: 'preset-student-02', nickname: '李静' },
  { testerKey: 'preset-student-03', nickname: '王磊' },
  { testerKey: 'preset-student-04', nickname: '刘芳' },
  { testerKey: 'preset-student-05', nickname: '陈伟' },
  { testerKey: 'preset-student-06', nickname: '杨敏' },
  { testerKey: 'preset-student-07', nickname: '赵强' },
  { testerKey: 'preset-student-08', nickname: '黄欣' },
  { testerKey: 'preset-student-09', nickname: '周宁' },
  { testerKey: 'preset-student-10', nickname: '吴悦' },
]

describe('test identities', () => {
  it('provides ten stable default student accounts', () => {
    const identities = mergeTestIdentities(defaultIdentities, null)
    expect(identities).toHaveLength(10)
    expect(identities[0]).toEqual({ testerKey: 'preset-student-01', nickname: '张明' })
    expect(identities[9]).toEqual({ testerKey: 'preset-student-10', nickname: '吴悦' })
  })

  it('keeps previously created identities after the defaults', () => {
    const identities = mergeTestIdentities(defaultIdentities, [{ testerKey: 'tester-existing', nickname: '原有学员' }])
    expect(identities).toHaveLength(11)
    expect(identities[10]).toEqual({ testerKey: 'tester-existing', nickname: '原有学员' })
  })

  it('ignores invalid and duplicate stored identities', () => {
    const identities = mergeTestIdentities(defaultIdentities, [
      defaultIdentities[0],
      { testerKey: '', nickname: '无效身份' },
      { testerKey: 'tester-valid', nickname: '有效身份' },
      { testerKey: 'tester-valid', nickname: '重复身份' },
    ])
    expect(identities).toHaveLength(11)
    expect(identities.at(-1)?.nickname).toBe('有效身份')
  })
})
