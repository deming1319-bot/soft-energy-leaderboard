import type { ActivityDetail, ActivitySummary, CreateReportPayload, Leaderboard, Profile, ReportItem, SubmissionResult } from '../types/index'
import { request } from './request'

export const activityApi = {
  list: () => request<ActivitySummary[]>({ url: '/miniapp/activities', method: 'GET' }),
  detail: (id: string) => request<ActivityDetail>({ url: `/miniapp/activities/${id}`, method: 'GET' }),
  submit: (id: string, answer: string, clientRequestId: string) => request<SubmissionResult>({ url: `/miniapp/activities/${id}/submissions`, method: 'POST', data: { answer, clientRequestId } }),
  mySubmission: (id: string) => request<SubmissionResult>({ url: `/miniapp/activities/${id}/my-submission`, method: 'GET' }),
  leaderboard: (id: string) => request<Leaderboard>({ url: `/miniapp/activities/${id}/leaderboard`, method: 'GET' }),
}
export const profileApi = {
  get: () => request<Profile>({ url: '/miniapp/profile', method: 'GET' }),
  update: (nickname: string) => request<Profile>({ url: '/miniapp/profile', method: 'PUT', data: { nickname } }),
  cancel: () => request<void>({ url: '/miniapp/profile', method: 'DELETE' }),
}
export const reportApi = {
  create: (payload: CreateReportPayload) => request<ReportItem>({ url: '/miniapp/reports', method: 'POST', data: payload }),
  mine: () => request<ReportItem[]>({ url: '/miniapp/reports/mine', method: 'GET' }),
}
