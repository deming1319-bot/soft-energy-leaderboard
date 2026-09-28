export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  timestamp: string
}

export type CultureCategory =
  | 'CLASSICS'
  | 'PHILOSOPHY'
  | 'LIFE_PRACTICE'
  | 'CONFUCIAN'
  | 'BUDDHIST'
  | 'TAOIST'
  | 'INTEGRATED'
  | 'MASTER_ORIGINAL'

export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED'
export type TimeState = 'DRAFT' | 'UPCOMING' | 'ACTIVE' | 'ENDED' | 'ARCHIVED'
export type SubmissionGrade = 'CHAMPION' | 'RUNNER_UP' | 'REVIEW_REQUIRED' | 'UNRANKED'
export type RevealMode = 'AFTER_DEADLINE' | 'AFTER_SUBMIT'
export type DeadlineMode = 'DURATION' | 'FIXED_TIME'
export type ReportType = 'ILLEGAL_CONTENT' | 'INFRINGEMENT' | 'INAPPROPRIATE_NICKNAME' | 'QUESTION_CONTENT' | 'SCORE_APPEAL' | 'PRIVACY' | 'OTHER'
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'
export type ReportTargetType = 'GENERAL' | 'ACTIVITY' | 'USER' | 'SUBMISSION'
export type ModerationAction = 'NONE' | 'RESET_NICKNAME' | 'DISABLE_USER'

export interface CurrentUser {
  id: string
  username: string | null
  displayName: string
  avatarUrl: string | null
  role: string
}

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

export interface DashboardOverview {
  totalUsers: number
  activeActivities: number
  totalSubmissions: number
  championCount: number
  runnerUpCount: number
  reviewRequiredCount: number
  recentSubmissions: Array<{
    id: string
    activityTitle: string
    userDisplayName: string
    grade: SubmissionGrade
    submittedAt: string
  }>
}

export interface ActivitySummary {
  id: string
  title: string
  subtitle: string | null
  category: CultureCategory
  status: ActivityStatus
  timeState: TimeState
  startAt: string
  endAt: string
  submissionCount: number
  championCount: number
  runnerUpCount: number
  updatedAt: string
}

export interface ActivityDetail {
  id: string
  title: string
  subtitle: string | null
  category: CultureCategory
  questionText: string
  sourceTitle: string | null
  sourceDetail: string | null
  standardAnswer: string
  answerPoints: string[]
  runnerUpThreshold: number
  deadlineMode: DeadlineMode
  durationHours: number | null
  startAt: string
  endAt: string
  status: ActivityStatus
  revealMode: RevealMode
  maxAttempts: number
  publishedAt: string | null
  closedAt: string | null
  version: number
}

export interface ActivityPayload {
  title: string
  subtitle: string
  category: CultureCategory
  questionText: string
  sourceTitle: string
  sourceDetail: string
  standardAnswer: string
  answerPoints: string[]
  runnerUpThreshold: number
  deadlineMode: DeadlineMode
  durationHours: number | null
  startAt: string
  endAt: string
  revealMode: RevealMode
  maxAttempts: number
}

export interface SubmissionItem {
  id: string
  userId: string
  userDisplayName: string
  maskedPhone: string
  attemptNo: number
  answerText: string
  originalGrade: SubmissionGrade
  grade: SubmissionGrade
  score: number
  matchedPoints: string[]
  submittedAt: string
  reviewedBy: string | null
  reviewedAt: string | null
  reviewNote: string | null
}

export interface LeaderboardEntry {
  rank: number
  userId: string
  displayName: string
  avatarUrl: string | null
  grade: SubmissionGrade
  submittedAt: string
  currentUser: boolean
}

export interface Leaderboard {
  activityId: string
  activityTitle: string
  revealed: boolean
  endAt: string
  champions: LeaderboardEntry[]
  runnersUp: LeaderboardEntry[]
  myResult?: SubmissionResult | null
}

export interface TestActivitySummary {
  id: string
  title: string
  subtitle: string | null
  category: CultureCategory
  timeState: Exclude<TimeState, 'DRAFT' | 'ARCHIVED'>
  startAt: string
  endAt: string
  participantCount: number
  submitted: boolean
}

export interface TestActivityDetail extends TestActivitySummary {
  questionText: string
  sourceTitle: string | null
  sourceDetail: string | null
  maxAttempts: number
  attemptsUsed: number
  canSubmit: boolean
}

export interface SubmissionResult {
  id: string
  activityId: string
  activityTitle: string
  grade: SubmissionGrade
  score: number
  matchedPoints: string[]
  submittedAt: string
  feedback: string
  answerRevealed: boolean
  standardAnswer: string | null
  reviewNote: string | null
}

export interface UserItem {
  id: string
  nickname: string
  avatarUrl: string | null
  maskedPhone: string
  status: string
  participationCount: number
  championCount: number
  runnerUpCount: number
  lastLoginAt: string | null
  createdAt: string
}

export interface ReportItem {
  id: string
  type: ReportType
  targetType: ReportTargetType
  targetId: string | null
  targetLabel: string
  reporterDisplayName: string
  description: string
  status: ReportStatus
  handlingNote: string | null
  handledBy: string | null
  handledAt: string | null
  createdAt: string
  updatedAt: string
}
