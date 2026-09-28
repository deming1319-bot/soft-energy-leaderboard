import { http } from './http'
import type {
  ActivityDetail,
  ActivityPayload,
  ActivitySummary,
  ApiResponse,
  DashboardOverview,
  Leaderboard,
  ModerationAction,
  ReportItem,
  ReportStatus,
  SubmissionGrade,
  SubmissionItem,
  SubmissionResult,
  TestActivityDetail,
  TestActivitySummary,
  TestAccount,
  TokenResponse,
  UserItem,
} from '@/types'

const unwrap = <T>(response: { data: ApiResponse<T> }) => response.data.data

export const authApi = {
  login: (username: string, password: string) =>
    http.post<ApiResponse<TokenResponse>>('/admin/auth/login', { username, password }).then(unwrap),
  me: () => http.get('/admin/auth/me').then(unwrap),
}

export const dashboardApi = {
  overview: () => http.get<ApiResponse<DashboardOverview>>('/admin/dashboard').then(unwrap),
}

export const activityApi = {
  list: () => http.get<ApiResponse<ActivitySummary[]>>('/admin/activities').then(unwrap),
  detail: (id: string) => http.get<ApiResponse<ActivityDetail>>(`/admin/activities/${id}`).then(unwrap),
  create: (payload: ActivityPayload) =>
    http.post<ApiResponse<ActivityDetail>>('/admin/activities', payload).then(unwrap),
  update: (id: string, payload: ActivityPayload) =>
    http.put<ApiResponse<ActivityDetail>>(`/admin/activities/${id}`, payload).then(unwrap),
  remove: (id: string) => http.delete<ApiResponse<void>>(`/admin/activities/${id}`).then(unwrap),
  publish: (id: string) => http.post(`/admin/activities/${id}/publish`).then(unwrap),
  close: (id: string) => http.post(`/admin/activities/${id}/close`).then(unwrap),
  submissions: (id: string) =>
    http.get<ApiResponse<SubmissionItem[]>>(`/admin/activities/${id}/submissions`).then(unwrap),
  leaderboard: (id: string) =>
    http.get<ApiResponse<Leaderboard>>(`/admin/activities/${id}/leaderboard`).then(unwrap),
}

export const submissionApi = {
  review: (id: string, grade: SubmissionGrade, note: string) =>
    http.post(`/admin/submissions/${id}/review`, { grade, note }).then(unwrap),
}

export const userApi = {
  list: () => http.get<ApiResponse<UserItem[]>>('/admin/users').then(unwrap),
}

export const reportApi = {
  list: (status?: ReportStatus) =>
    http.get<ApiResponse<ReportItem[]>>('/admin/reports', { params: status ? { status } : undefined }).then(unwrap),
  handle: (id: string, status: ReportStatus, note: string, action: ModerationAction) =>
    http.post<ApiResponse<ReportItem>>(`/admin/reports/${id}/handle`, { status, note, action }).then(unwrap),
}

export const auditApi = {
  list: () => http.get<ApiResponse<Record<string, unknown>[]>>('/admin/audit-logs').then(unwrap),
}

const studentHeaders = (token: string) => ({ Authorization: `Bearer ${token}` })

export const testCenterApi = {
  accounts: () =>
    http.get<ApiResponse<TestAccount[]>>('/admin/test-center/accounts').then(unwrap),
  startSession: (nickname: string, testerKey: string) =>
    http.post<ApiResponse<TokenResponse>>('/admin/test-center/session', { nickname, testerKey }).then(unwrap),
  activities: (token: string) =>
    http.get<ApiResponse<TestActivitySummary[]>>('/miniapp/activities', { headers: studentHeaders(token) }).then(unwrap),
  detail: (token: string, id: string) =>
    http.get<ApiResponse<TestActivityDetail>>(`/miniapp/activities/${id}`, { headers: studentHeaders(token) }).then(unwrap),
  submit: (token: string, id: string, answer: string, clientRequestId: string) =>
    http.post<ApiResponse<SubmissionResult>>(
      `/miniapp/activities/${id}/submissions`,
      { answer, clientRequestId },
      { headers: studentHeaders(token) },
    ).then(unwrap),
  mySubmission: (token: string, id: string) =>
    http.get<ApiResponse<SubmissionResult>>(`/miniapp/activities/${id}/my-submission`, { headers: studentHeaders(token) }).then(unwrap),
  leaderboard: (token: string, id: string) =>
    http.get<ApiResponse<Leaderboard>>(`/miniapp/activities/${id}/leaderboard`, { headers: studentHeaders(token) }).then(unwrap),
}
