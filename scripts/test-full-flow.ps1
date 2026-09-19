param(
    [string]$BaseUrl = 'http://127.0.0.1:8081/api/v1',
    [string]$AdminUsername = 'admin',
    [string]$AdminPassword = '123456'
)

$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = ''
    )
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    return Invoke-RestMethod @parameters
}

function Assert-Flow {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "FLOW ASSERTION FAILED: $Message" }
    Write-Host "PASS: $Message"
}

Write-Host '[1/9] Admin login'
$adminLogin = Invoke-Api -Method Post -Path '/admin/auth/login' -Body @{
    username = $AdminUsername
    password = $AdminPassword
}
$adminToken = $adminLogin.data.accessToken
Assert-Flow ($adminLogin.code -eq 'OK') 'admin can log in'

Write-Host '[2/9] Create and publish an activity'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$activityPayload = @{
    title = "FLOW-TEST-$stamp"
    subtitle = 'Publish, answer, score, close, and rank acceptance flow'
    category = 'CLASSICS'
    questionText = 'Write the two lines exactly in the required order.'
    sourceTitle = 'Automated acceptance test'
    sourceDetail = 'Safe test data; may remain as a closed record.'
    standardAnswer = "alpha beta`ngamma delta"
    answerPoints = @('alpha beta', 'gamma delta')
    runnerUpThreshold = 0.9
    deadlineMode = 'DURATION'
    durationHours = 12
    startAt = (Get-Date).AddMinutes(-1).ToUniversalTime().ToString('o')
    endAt = (Get-Date).AddHours(12).ToUniversalTime().ToString('o')
    revealMode = 'AFTER_DEADLINE'
    maxAttempts = 1
}
$created = Invoke-Api -Method Post -Path '/admin/activities' -Body $activityPayload -Token $adminToken
$activityId = $created.data.id
$published = Invoke-Api -Method Post -Path "/admin/activities/$activityId/publish" -Token $adminToken
Assert-Flow ($published.data.status -eq 'PUBLISHED') 'draft can be published'
Assert-Flow ($published.data.deadlineMode -eq 'DURATION' -and $published.data.durationHours -eq 12) 'default duration mode keeps a 12-hour answer window'

function Login-Student {
    param([string]$DeviceId, [string]$Nickname)
    $login = Invoke-Api -Method Post -Path '/miniapp/auth/login' -Body @{
        code = 'flow-test-code'
        deviceId = $DeviceId
        agreementAccepted = $true
        privacyVersion = '2026-08-02'
        termsVersion = '2026-08-02'
    }
    Invoke-Api -Method Put -Path '/miniapp/profile' -Body @{ nickname = $Nickname } -Token $login.data.accessToken | Out-Null
    return $login
}

function Submit-Answer {
    param([object]$Login, [string]$Answer, [string]$RequestId)
    return Invoke-Api -Method Post -Path "/miniapp/activities/$activityId/submissions" -Body @{
        answer = $Answer
        clientRequestId = $RequestId
    } -Token $Login.data.accessToken
}

Write-Host '[3/9] Simulate five independent students'
$unique = [Guid]::NewGuid().ToString('N')
$championA = Login-Student -DeviceId "flow-a-$unique" -Nickname 'Flow Champion A'
$runner = Login-Student -DeviceId "flow-runner-$unique" -Nickname 'Flow Runner'
$unranked = Login-Student -DeviceId "flow-unranked-$unique" -Nickname 'Flow Unranked'
$championB = Login-Student -DeviceId "flow-b-$unique" -Nickname 'Flow Champion B'
$lateStudent = Login-Student -DeviceId "flow-late-$unique" -Nickname 'Flow Late'

Write-Host '[4/9] Submit champion, runner-up, unranked, champion in sequence'
$smileEmoji = [char]::ConvertFromUtf32(0x1F60A)
$resultA = Submit-Answer -Login $championA -Answer "alpha beta$smileEmoji`ngamma delta" -RequestId "a-$unique"
$sameRequestResult = Submit-Answer -Login $championA -Answer "alpha beta$smileEmoji`ngamma delta" -RequestId "a-$unique"
Start-Sleep -Milliseconds 150
$resultRunner = Submit-Answer -Login $runner -Answer "gamma delta`nalpha beta" -RequestId "r-$unique"
Start-Sleep -Milliseconds 150
$resultUnranked = Submit-Answer -Login $unranked -Answer 'nothing relevant' -RequestId "u-$unique"
Start-Sleep -Milliseconds 150
$resultB = Submit-Answer -Login $championB -Answer "alpha beta`ngamma delta" -RequestId "b-$unique"

Assert-Flow ($resultA.data.grade -eq 'CHAMPION') 'exact answer remains champion when it contains an emoji'
Assert-Flow ($sameRequestResult.data.id -eq $resultA.data.id) 'repeated client request is idempotent'
Assert-Flow ($resultRunner.data.grade -eq 'RUNNER_UP') 'two exact lines in reversed order are runner-up'
Assert-Flow ([decimal]$resultRunner.data.score -eq [decimal]0.9) 'wrong-order score includes an order penalty'
Assert-Flow ($resultUnranked.data.grade -eq 'UNRANKED') 'unrelated answer is unranked'
Assert-Flow ($resultB.data.grade -eq 'CHAMPION') 'later exact answer is also champion'
Assert-Flow (-not $resultA.data.answerRevealed) 'standard answer is hidden before deadline'
$attemptBlocked = $false
try {
    $null = Submit-Answer -Login $championA -Answer "alpha beta`ngamma delta" -RequestId "a-second-$unique"
} catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409) {
        $attemptBlocked = $true
    } else {
        throw
    }
}
Assert-Flow $attemptBlocked 'a second distinct attempt is blocked when maxAttempts is one'

Write-Host '[5/9] Verify leaderboard is hidden before close'
$beforeClose = Invoke-Api -Method Get -Path "/miniapp/activities/$activityId/leaderboard" -Token $championA.data.accessToken
Assert-Flow (-not $beforeClose.data.revealed) 'student leaderboard is hidden before close'
Assert-Flow ($beforeClose.data.champions.Count -eq 0) 'hidden leaderboard returns no champion list'

Write-Host '[6/9] Close activity and verify final ranking'
$closed = Invoke-Api -Method Post -Path "/admin/activities/$activityId/close" -Token $adminToken
Assert-Flow ($closed.data.status -eq 'CLOSED') 'published activity can be closed'
$board = Invoke-Api -Method Get -Path "/miniapp/activities/$activityId/leaderboard" -Token $championA.data.accessToken
$adminSubmissions = Invoke-Api -Method Get -Path "/admin/activities/$activityId/submissions" -Token $adminToken

Assert-Flow ($board.data.revealed) 'leaderboard is revealed after close'
Assert-Flow ($board.data.myResult.answerRevealed) 'standard answer is revealed after close'
Assert-Flow ($board.data.myResult.standardAnswer -eq "alpha beta`ngamma delta") 'revealed two-line standard answer is correct'
Assert-Flow ($board.data.champions.Count -eq 2) 'two champions are listed'
Assert-Flow ($board.data.champions[0].displayName -eq 'Flow Champion A') 'earlier champion ranks first'
Assert-Flow ($board.data.champions[1].displayName -eq 'Flow Champion B') 'later champion ranks second'
Assert-Flow ($board.data.champions[0].rank -eq 1 -and $board.data.champions[1].rank -eq 2) 'champion rank numbers are stable'
Assert-Flow ($board.data.runnersUp.Count -eq 1) 'one runner-up is listed'
Assert-Flow ($board.data.runnersUp[0].displayName -eq 'Flow Runner') 'runner-up identity is correct'
Assert-Flow ($adminSubmissions.data.Count -eq 4) 'admin sees all four submissions'
Assert-Flow (($board.data.champions.displayName -notcontains 'Flow Unranked') -and ($board.data.runnersUp.displayName -notcontains 'Flow Unranked')) 'unranked student is excluded from public ranks'
$lateBlocked = $false
try {
    $null = Submit-Answer -Login $lateStudent -Answer "alpha beta`ngamma delta" -RequestId "late-$unique"
} catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409) { $lateBlocked = $true } else { throw }
}
Assert-Flow $lateBlocked 'a new student cannot submit after the deadline'

Write-Host '[7/9] Submit and resolve a report ticket'
$report = Invoke-Api -Method Post -Path '/miniapp/reports' -Body @{
    type = 'QUESTION_CONTENT'
    activityId = $activityId
    description = 'Acceptance test: verify the source note.'
} -Token $championA.data.accessToken
Assert-Flow ($report.data.status -eq 'PENDING') 'student can submit a report ticket'
$pendingReports = Invoke-Api -Method Get -Path '/admin/reports?status=PENDING' -Token $adminToken
Assert-Flow (($pendingReports.data.id -contains $report.data.id)) 'admin can see the pending report'
$handled = Invoke-Api -Method Post -Path "/admin/reports/$($report.data.id)/handle" -Body @{
    status = 'RESOLVED'
    action = 'NONE'
    note = 'Acceptance report resolved.'
} -Token $adminToken
Assert-Flow ($handled.data.status -eq 'RESOLVED') 'admin can resolve the report'
$myReports = Invoke-Api -Method Get -Path '/miniapp/reports/mine' -Token $championA.data.accessToken
Assert-Flow (($myReports.data | Where-Object id -eq $report.data.id).handlingNote -eq 'Acceptance report resolved.') 'student can read the handling result'

Write-Host '[8/9] Cancel an account and verify irreversible leaderboard anonymization'
Invoke-Api -Method Delete -Path '/miniapp/profile' -Token $championB.data.accessToken | Out-Null
$boardAfterErasure = Invoke-Api -Method Get -Path "/miniapp/activities/$activityId/leaderboard" -Token $championA.data.accessToken
Assert-Flow ($boardAfterErasure.data.champions.Count -eq 2) 'cancellation keeps the published rank count'
Assert-Flow ($boardAfterErasure.data.champions[1].displayName -ne 'Flow Champion B') 'cancelled champion is shown without identity'
Assert-Flow ($boardAfterErasure.data.champions[1].userId.StartsWith('anonymous:')) 'anonymous rank has no reusable user id'
$oldTokenRejected = $false
try {
    $null = Invoke-Api -Method Get -Path '/miniapp/profile' -Token $championB.data.accessToken
} catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404) { $oldTokenRejected = $true } else { throw }
}
Assert-Flow $oldTokenRejected 'cancelled account token cannot access personal data'
$submissionsAfterErasure = Invoke-Api -Method Get -Path "/admin/activities/$activityId/submissions" -Token $adminToken
Assert-Flow ($submissionsAfterErasure.data.Count -eq 3) 'cancelled account original answer is deleted from admin records'

Write-Host '[9/9] Full flow passed'
[pscustomobject]@{
    status = 'PASSED'
    activityId = $activityId
    activityTitle = $created.data.title
    adminDetailUrl = "http://127.0.0.1:5173/activities/$activityId"
    submissions = $submissionsAfterErasure.data.Count
    champions = $boardAfterErasure.data.champions.Count
    runnersUp = $board.data.runnersUp.Count
    reportWorkflow = 'RESOLVED'
    cancelledUser = 'ANONYMIZED_AND_ERASED'
    unrankedExcluded = $true
} | ConvertTo-Json -Depth 4
