import type { TestAccount } from '@/types'

export type TestIdentity = TestAccount

export function mergeTestIdentities(defaults: TestIdentity[], stored: unknown): TestIdentity[] {
  const identities = defaults.map((identity) => ({ ...identity }))
  const existingKeys = new Set(identities.map((identity) => identity.testerKey))
  if (!Array.isArray(stored)) return identities

  stored.forEach((candidate) => {
    if (!candidate || typeof candidate !== 'object') return
    const testerKey = 'testerKey' in candidate && typeof candidate.testerKey === 'string' ? candidate.testerKey.trim() : ''
    const nicknameValue = 'nickname' in candidate && typeof candidate.nickname === 'string' ? candidate.nickname.trim() : ''
    if (!testerKey || !nicknameValue || existingKeys.has(testerKey)) return
    const nickname = nicknameValue === '体验一号'
      ? `体验-${testerKey.replace(/[^a-z0-9]/gi, '').slice(-4).toUpperCase()}`
      : nicknameValue
    identities.push({ testerKey, nickname })
    existingKeys.add(testerKey)
  })

  return identities
}
