const HOME_PATH = '/pages/home/index'

export function safePagePath(value?: string): string {
  if (!value) return HOME_PATH
  try {
    const decoded = decodeURIComponent(value)
    return /^\/pages\/[a-z0-9-]+\/index(?:\?.*)?$/i.test(decoded) ? decoded : HOME_PATH
  } catch {
    return HOME_PATH
  }
}

export function loginPageUrl(redirect = HOME_PATH): string {
  return `/pages/welcome/index?redirect=${encodeURIComponent(safePagePath(redirect))}`
}
