import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/services'
import type { CurrentUser } from '@/types'

const TOKEN_KEY = 'soft-energy-admin-token'
const USER_KEY = 'soft-energy-admin-user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const stored = localStorage.getItem(USER_KEY)
  const user = ref<CurrentUser | null>(stored ? JSON.parse(stored) : null)
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(username: string, password: string) {
    const response = await authApi.login(username, password)
    token.value = response.accessToken
    user.value = response.currentUser
    localStorage.setItem(TOKEN_KEY, response.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(response.currentUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, login, logout }
})

