interface IAppOption {
  globalData: {
    apiBaseUrl: string
    environment: 'develop' | 'trial' | 'release'
    runtimeReady: boolean
    networkConnected: boolean
    user: CurrentUser | null
    testAdminToken: string | null
    testAdminExpiresAt: string | null
  }
}

interface CurrentUser {
  id: string
  displayName: string
  avatarUrl: string | null
  maskedPhone?: string
  role: string
  profileComplete: boolean
}
