const LARGE_READING_KEY = 'soft-energy-large-reading'

export function isLargeReading(): boolean {
  return wx.getStorageSync<boolean>(LARGE_READING_KEY) === true
}

export function setLargeReading(enabled: boolean): void {
  wx.setStorageSync(LARGE_READING_KEY, enabled)
}

