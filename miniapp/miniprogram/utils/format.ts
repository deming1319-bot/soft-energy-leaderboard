import type { CultureCategory, Grade, TimeState } from '../types/index'

export const categoryLabels: Record<CultureCategory, string> = { CLASSICS: '经', PHILOSOPHY: '思', LIFE_PRACTICE: '行', CONFUCIAN: '文', BUDDHIST: '文', TAOIST: '文', INTEGRATED: '文', MASTER_ORIGINAL: '文' }
export const timeStateLabels: Record<TimeState, string> = { UPCOMING: '即将开始', ACTIVE: '正在进行', ENDED: '已经结束' }
export const gradeLabels: Record<Grade, string> = { CHAMPION: '冠军', RUNNER_UP: '亚军', REVIEW_REQUIRED: '待复核', UNRANKED: '继续精进' }

export function formatDateTime(value: string): string {
  const date = new Date(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}月${pad(date.getDate())}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function remainingText(endAt: string, now = Date.now()): string {
  const diff = new Date(endAt).getTime() - now
  if (diff <= 0) return '已截止'
  const hours = Math.floor(diff / 3_600_000)
  const minutes = Math.max(1, Math.floor((diff % 3_600_000) / 60_000))
  return hours > 0 ? `余 ${hours} 小时 ${minutes} 分` : `余 ${minutes} 分钟`
}
