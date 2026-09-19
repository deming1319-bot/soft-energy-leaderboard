import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResponse } from '@/types'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('soft-energy-admin-token')
  const isLoginRequest = config.url?.endsWith('/admin/auth/login')
  if (token && !isLoginRequest && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const isAdminRequest = error.config?.url?.startsWith('/admin/')
    if (status === 401 && isAdminRequest) {
      localStorage.removeItem('soft-energy-admin-token')
      localStorage.removeItem('soft-energy-admin-user')
      if (router.currentRoute.value.name !== 'login') await router.replace({ name: 'login' })
    }
    const message = error.response?.data?.message
      || (status === 401 ? '账号或密码不正确' : status ? '请求处理失败' : '无法连接服务，请检查后端是否启动')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)
