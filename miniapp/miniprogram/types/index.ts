export type TimeState = 'UPCOMING' | 'ACTIVE' | 'ENDED'
export type Grade = 'CHAMPION' | 'RUNNER_UP' | 'REVIEW_REQUIRED' | 'UNRANKED'
export type CultureCategory = 'CLASSICS' | 'PHILOSOPHY' | 'LIFE_PRACTICE' | 'CONFUCIAN' | 'BUDDHIST' | 'TAOIST' | 'INTEGRATED' | 'MASTER_ORIGINAL'
export type ReportType = 'ILLEGAL_CONTENT' | 'INFRINGEMENT' | 'INAPPROPRIATE_NICKNAME' | 'QUESTION_CONTENT' | 'SCORE_APPEAL' | 'PRIVACY' | 'OTHER'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'
export type ReportTargetType = 'GENERAL' | 'ACTIVITY' | 'USER' | 'SUBMISSION'

export interface TokenResponse {
  accessToken: string
  expiresAt: string
  tokenType: string
  currentUser: CurrentUser
}

export interface TestAccount {
  testerKey: string
  nickname: string
}

export interface ActiveTestIdentity extends TestAccount {
  currentUser: CurrentUser
}

export interface ActivitySummary {
  id: string; title: string; subtitle: string | null; category: CultureCategory; timeState: TimeState
  startAt: string; endAt: string; participantCount: number; submitted: boolean
}
export interface ActivityDetail extends ActivitySummary {
  questionText: string; sourceTitle: string | null; sourceDetail: string | null
  maxAttempts: number; attemptsUsed: number; canSubmit: boolean
}
export interface SubmissionResult {
  id: string; activityId: string; activityTitle: string; grade: Grade; score: number
  matchedPoints: string[]; submittedAt: string; feedback: string; answerRevealed: boolean
  standardAnswer: string | null; reviewNote: string | null
}
export interface LeaderboardEntry {
  rank: number; userId: string; displayName: string; avatarUrl: string | null
  grade: Grade; submittedAt: string; currentUser: boolean
}
export interface Leaderboard {
  activityId: string; activityTitle: string; revealed: boolean; endAt: string
  champions: LeaderboardEntry[]; runnersUp: LeaderboardEntry[]; myResult: SubmissionResult | null
}
export interface Profile {
  id: string; nickname: string; avatarUrl: string | null; maskedPhone: string
  profileComplete: boolean; privacyVersion: string | null; termsVersion: string | null; consentedAt: string | null
  participationCount: number; championCount: number; runnerUpCount: number; recentResults: SubmissionResult[]
}
export interface ReportItem {
  id: string; type: ReportType; targetType: ReportTargetType; targetId: string | null; targetLabel: string
  reporterDisplayName: string; description: string; status: ReportStatus; handlingNote: string | null
  handledBy: string | null; handledAt: string | null; createdAt: string; updatedAt: string
}
export interface CreateReportPayload {
  type: ReportType; activityId?: string; targetUserId?: string; submissionId?: string; description: string
}
export interface ApiResponse<T> { code: string; message: string; data: T; timestamp: string }
