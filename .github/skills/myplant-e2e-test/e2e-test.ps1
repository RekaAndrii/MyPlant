param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ProjectRoot = ""
)

# Auto-detect project root if not provided
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    # Go up 3 levels from this script: .github/skills/myplant-e2e-test/e2e-test.ps1
    $ProjectRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
}

# Verify project root exists
if (-not (Test-Path "$ProjectRoot/mvnw.cmd")) {
    Write-Host "Error: Cannot find mvnw.cmd in $ProjectRoot" -ForegroundColor Red
    Write-Host "ProjectRoot detection failed. Please provide -ProjectRoot explicitly." -ForegroundColor Red
    exit 1
}

# Constants
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$TEST_EMAIL = "e2e-$TIMESTAMP@test.local"
$TEST_USERNAME = "e2euser$TIMESTAMP"
$TEST_PASSWORD = "E2ePass1!"
$BLOCK_NAME = "E2ETestBlock"
$HEALTH_CHECK_TIMEOUT_SEC = 120
$HEALTH_POLL_INTERVAL_SEC = 3

# Report tracking
$report = @()
$overallStatus = "PASS"

# Helper function to add report entry
function Add-ReportEntry {
    param(
        [string]$Phase,
        [string]$Status,
        [string]$HttpCode = "",
        [string]$Note = ""
    )
    $entry = @{
        Phase = $Phase
        Status = $Status
        HttpCode = $HttpCode
        Note = $Note
    }
    $script:report += $entry
    
    if ($Status -eq "FAIL") {
        $script:overallStatus = "FAIL"
    }
}

# Helper function to make HTTP requests with cookies
function Invoke-AppRequest {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = ""
    )
    
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            UseBasicParsing = $true
        }

        if ($Headers.Count -gt 0) {
            $params["Headers"] = $Headers
        }
        
        if ($Method -eq "POST" -or $Method -eq "DELETE" -or $Method -eq "PUT") {
            $params["ContentType"] = "application/json"
        }
        
        if (-not [string]::IsNullOrWhiteSpace($Body)) {
            $params["Body"] = $Body
        }
        
        $response = Invoke-WebRequest @params -ErrorAction Stop
        
        return @{
            StatusCode = $response.StatusCode
            Content = $response.Content
            Headers = $response.Headers
            Success = $true
        }
    }
    catch {
        $errorResponse = $_
        if ($errorResponse.Exception.Response) {
            $statusCode = [int]$errorResponse.Exception.Response.StatusCode
            return @{
                StatusCode = $statusCode
                Content = ""
                Headers = @{}
                Success = $false
                Error = $errorResponse.Exception.Message
            }
        }
        return @{
            StatusCode = 0
            Content = $errorResponse.Exception.Message
            Headers = @{}
            Success = $false
            Error = $errorResponse.Exception.Message
         }
    }
}

# ==================== PRINT REPORT ====================

function Print-Report {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Test Report" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    # Print table header
    Write-Host ('{0,-20} {1,-10} {2,-8} {3,-40}' -f 'PHASE', 'STATUS', 'CODE', 'NOTE') -ForegroundColor White -BackgroundColor DarkGray
    
    # Print report entries
    foreach ($entry in $script:report) {
        $statusColor = if ($entry.Status -eq "PASS") { [System.ConsoleColor]::Green } else { [System.ConsoleColor]::Red }
        Write-Host ('{0,-20} {1,-10} {2,-8} {3,-40}' -f $entry.Phase, $entry.Status, $entry.HttpCode, $entry.Note) -ForegroundColor $statusColor
    }
    
    Write-Host "========================================" -ForegroundColor Cyan
    
    $overallColor = if ($script:overallStatus -eq "PASS") { [System.ConsoleColor]::Green } else { [System.ConsoleColor]::Red }
    Write-Host "  OVERALL: $($script:overallStatus)" -ForegroundColor $overallColor
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

# Helper function to convert response content to string
function ConvertResponseToString {
    param([object]$Content)
    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }
    return $Content
}

# Function to kill process on port 8080
function Stop-ProcessOnPort8080 {
    try {
        $port8080Process = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($port8080Process) {
            $processId = $port8080Process.OwningProcess
            $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
            if ($process) {
                Write-Host "Stopping process on port 8080 (PID: $processId)..."
                Stop-Process -Id $processId -Force
                Start-Sleep -Seconds 2
                Write-Host "Process stopped."
            }
        }
    }
    catch {
        Write-Host "Note: Could not check/kill port 8080 process: $($_.Exception.Message)"
    }
}

# Function to start the MyPlant app
function Start-MyPlantApp {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Starting MyPlant App" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    Stop-ProcessOnPort8080
    
    $startTime = Get-Date
    
    # Start Spring Boot app as background job
    Write-Host "Launching: cd $ProjectRoot && mvnw.cmd spring-boot:run"
    
    $job = Start-Job -ScriptBlock {
        param($ProjectRoot)
        Push-Location $ProjectRoot
        & .\mvnw.cmd spring-boot:run 2>&1
        Pop-Location
    } -ArgumentList $ProjectRoot
    
    $appStarted = $false
    $elapsedSec = 0
    
    Write-Host "Waiting for app to start..."
    
    while ($elapsedSec -lt $HEALTH_CHECK_TIMEOUT_SEC -and -not $appStarted) {
        Start-Sleep -Seconds $HEALTH_POLL_INTERVAL_SEC
        $elapsedSec += $HEALTH_POLL_INTERVAL_SEC
        
        # Print job output
        $jobOutput = Receive-Job -Job $job -Keep
        if ($jobOutput) {
            Write-Host $jobOutput -ForegroundColor DarkGray
        }
        
        # Check health endpoint
        $healthResponse = Invoke-AppRequest -Uri "$BaseUrl/actuator/health" -Method "GET"
        if ($healthResponse.Success) {
            $content = ConvertResponseToString $healthResponse.Content
            $healthData = ConvertFrom-Json $content
            if ($healthData.status -eq "UP") {
                $appStarted = $true
                Write-Host "[OK] App is UP" -ForegroundColor Green
            }
        }
    }
    
    if (-not $appStarted) {
        Write-Host "[FAIL] App failed to start within $HEALTH_CHECK_TIMEOUT_SEC seconds" -ForegroundColor Red
        Add-ReportEntry "START APP" "FAIL" "0" "Timeout waiting for app"
        return $false
    }
    
    $startupTime = [math]::Round(((Get-Date) - $startTime).TotalSeconds)
    Add-ReportEntry "START APP" "PASS" "200" "UP in ${startupTime}s"
    
    # Store job ID for cleanup
    $script:appJobId = $job.Id
    return $true
}

# ==================== START E2E TEST ====================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MyPlant E2E Test  $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Test Email: $TEST_EMAIL"
Write-Host "Test Username: $TEST_USERNAME"
Write-Host ""

# Phase 0: Start App
if (-not (Start-MyPlantApp)) {
    Write-Host ""
    Write-Host "Failed to start app. Exiting." -ForegroundColor Red
    Write-Host ""
    Print-Report
    exit 1
}

Write-Host ""

# Phase 1: Register
Write-Host "PHASE 1: Register user..." -ForegroundColor Cyan
$registerUri = "$BaseUrl/register"
$registerBody = "userName=$TEST_USERNAME&email=$TEST_EMAIL&password=$TEST_PASSWORD"
$webSession = $null

try {
    $regResponse = Invoke-WebRequest -Uri $registerUri -Method POST `
        -Body $registerBody -ContentType "application/x-www-form-urlencoded" `
        -UseBasicParsing -SessionVariable webSession -ErrorAction Stop
    # If we get here without redirect (e.g. 200 with error page), check final URL
    if ($regResponse.BaseResponse.ResponseUri -match "login" -or $regResponse.BaseResponse.RequestMessage.RequestUri -match "login") {
        Add-ReportEntry "REGISTER" "PASS" "302" ""
        Write-Host "[OK] User registered" -ForegroundColor Green
    } else {
        Add-ReportEntry "REGISTER" "FAIL" $regResponse.StatusCode "Unexpected response"
        Write-Host "[FAIL] Registration unexpected response (HTTP $($regResponse.StatusCode))" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "REGISTER" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Registration error: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 2: Login
Write-Host ""
Write-Host "PHASE 2: Login..." -ForegroundColor Cyan

$loginUri = "$BaseUrl/login"
$loginBody = "username=$TEST_EMAIL&password=$TEST_PASSWORD"
$sessionCookie = $null

try {
    $loginResponse = Invoke-WebRequest -Uri $loginUri -Method POST `
        -Body $loginBody -ContentType "application/x-www-form-urlencoded" `
        -UseBasicParsing -WebSession $webSession -ErrorAction Stop

    # Extract JSESSIONID from session cookies
    if ($webSession -and $webSession.Cookies) {
        $allCookies = $webSession.Cookies.GetCookies($loginUri)
        $jsession = $allCookies | Where-Object { $_.Name -eq "JSESSIONID" }
        if ($jsession) {
            $sessionCookie = $jsession.Value
        }
    }

    # Check we landed on /home
    $finalUri = ""
    try { $finalUri = $loginResponse.BaseResponse.ResponseUri.ToString() } catch {}
    try { $finalUri = $loginResponse.BaseResponse.RequestMessage.RequestUri.ToString() } catch {}

    if ($sessionCookie) {
        Add-ReportEntry "LOGIN" "PASS" "302" "-> /home"
        Write-Host "[OK] Login successful, JSESSIONID: $sessionCookie" -ForegroundColor Green
    } else {
        Add-ReportEntry "LOGIN" "FAIL" $loginResponse.StatusCode "JSESSIONID not found in session"
        Write-Host "[FAIL] Login failed - no JSESSIONID (HTTP $($loginResponse.StatusCode)) finalUri=$finalUri" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "LOGIN" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Login error: $($_.Exception.Message)" -ForegroundColor Red
}

if (-not $sessionCookie) {
    Write-Host ""
    Write-Host "Cannot continue without session. Stopping." -ForegroundColor Red
    Print-Report
    exit 1
}

# Phase 3: Create Block
Write-Host ""
Write-Host "PHASE 3: Create block..." -ForegroundColor Cyan

$blockUri = "$BaseUrl/block/"
$blockBody = @{ name = $BLOCK_NAME } | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri $blockUri -Method POST -Body $blockBody `
        -ContentType "application/json" -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if (-not $respData.hasError) {
        Add-ReportEntry "CREATE BLOCK" "PASS" $resp.StatusCode "{hasError:false}"
        Write-Host "[OK] Block created" -ForegroundColor Green
    } else {
        Add-ReportEntry "CREATE BLOCK" "FAIL" $resp.StatusCode "hasError=true"
        Write-Host "[FAIL] Block creation returned error" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "CREATE BLOCK" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Block creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4: Execute Block
Write-Host ""
Write-Host "PHASE 4: Execute block..." -ForegroundColor Cyan

$executeUri = "$BaseUrl/block/execute?name=$BLOCK_NAME"
try {
    $resp = Invoke-WebRequest -Uri $executeUri -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if (-not $respData.hasError) {
        Add-ReportEntry "EXECUTE" "PASS" $resp.StatusCode "{hasError:false}"
        Write-Host "[OK] Block executed" -ForegroundColor Green
    } else {
        Add-ReportEntry "EXECUTE" "FAIL" $resp.StatusCode "hasError=true"
        Write-Host "[FAIL] Block execution returned error" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "EXECUTE" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Block execution failed: $($_.Exception.Message)" -ForegroundColor Red
}

# ==================== CHALLENGE BLOCK PHASES ====================

$CHALLENGE_BLOCK_NAME = "E2EChallengeBlock"
$CHALLENGE_TARGET = 3

# Phase 4a: Create Challenge Block
Write-Host ""
Write-Host "PHASE 4a: Create challenge block..." -ForegroundColor Cyan

$challengeBody = @{ name = $CHALLENGE_BLOCK_NAME; isChallenge = $true; targetExecutions = $CHALLENGE_TARGET } | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/block/" -Method POST -Body $challengeBody `
        -ContentType "application/json" -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if (-not $respData.hasError) {
        Add-ReportEntry "CREATE CHALLENGE" "PASS" $resp.StatusCode "{hasError:false}"
        Write-Host "[OK] Challenge block created (target=$CHALLENGE_TARGET)" -ForegroundColor Green
    } else {
        Add-ReportEntry "CREATE CHALLENGE" "FAIL" $resp.StatusCode "hasError=true"
        Write-Host "[FAIL] Challenge block creation returned error" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "CREATE CHALLENGE" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Challenge block creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4b: Execute Challenge Block 1/3
Write-Host ""
Write-Host "PHASE 4b: Execute challenge block (1/$CHALLENGE_TARGET)..." -ForegroundColor Cyan

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/block/execute?name=$CHALLENGE_BLOCK_NAME" -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    $expectedRemaining = $CHALLENGE_TARGET - 1
    if (-not $respData.hasError -and $respData.isChallenge -and $respData.remainingExecutions -eq $expectedRemaining -and -not $respData.completed) {
        Add-ReportEntry "EXEC CHALLENGE 1/$CHALLENGE_TARGET" "PASS" $resp.StatusCode "remaining=$($respData.remainingExecutions)"
        Write-Host "[OK] remaining=$($respData.remainingExecutions), completed=$($respData.completed)" -ForegroundColor Green
    } else {
        Add-ReportEntry "EXEC CHALLENGE 1/$CHALLENGE_TARGET" "FAIL" $resp.StatusCode "remaining=$($respData.remainingExecutions) completed=$($respData.completed)"
        Write-Host "[FAIL] Unexpected response: remaining=$($respData.remainingExecutions) completed=$($respData.completed)" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "EXEC CHALLENGE 1/$CHALLENGE_TARGET" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4c: Execute Challenge Block 2/3
Write-Host ""
Write-Host "PHASE 4c: Execute challenge block (2/$CHALLENGE_TARGET)..." -ForegroundColor Cyan

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/block/execute?name=$CHALLENGE_BLOCK_NAME" -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    $expectedRemaining = $CHALLENGE_TARGET - 2
    if (-not $respData.hasError -and $respData.isChallenge -and $respData.remainingExecutions -eq $expectedRemaining -and -not $respData.completed) {
        Add-ReportEntry "EXEC CHALLENGE 2/$CHALLENGE_TARGET" "PASS" $resp.StatusCode "remaining=$($respData.remainingExecutions)"
        Write-Host "[OK] remaining=$($respData.remainingExecutions), completed=$($respData.completed)" -ForegroundColor Green
    } else {
        Add-ReportEntry "EXEC CHALLENGE 2/$CHALLENGE_TARGET" "FAIL" $resp.StatusCode "remaining=$($respData.remainingExecutions) completed=$($respData.completed)"
        Write-Host "[FAIL] Unexpected response: remaining=$($respData.remainingExecutions) completed=$($respData.completed)" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "EXEC CHALLENGE 2/$CHALLENGE_TARGET" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4d: Execute Challenge Block 3/3 - should complete
Write-Host ""
Write-Host "PHASE 4d: Execute challenge block (3/$CHALLENGE_TARGET - completes)..." -ForegroundColor Cyan

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/block/execute?name=$CHALLENGE_BLOCK_NAME" -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if (-not $respData.hasError -and $respData.isChallenge -and $respData.completed -and $respData.remainingExecutions -eq 0) {
        Add-ReportEntry "EXEC CHALLENGE 3/$CHALLENGE_TARGET" "PASS" $resp.StatusCode "completed=true"
        Write-Host "[OK] Challenge completed! remaining=0, completed=true" -ForegroundColor Green
    } else {
        Add-ReportEntry "EXEC CHALLENGE 3/$CHALLENGE_TARGET" "FAIL" $resp.StatusCode "remaining=$($respData.remainingExecutions) completed=$($respData.completed)"
        Write-Host "[FAIL] Unexpected response: remaining=$($respData.remainingExecutions) completed=$($respData.completed)" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "EXEC CHALLENGE 3/$CHALLENGE_TARGET" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4e: Verify Achievement saved
Write-Host ""
Write-Host "PHASE 4e: Verify achievement saved..." -ForegroundColor Cyan

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/achievements/all" -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $achievements = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    $found = $achievements | Where-Object { $_.goalName -eq $CHALLENGE_BLOCK_NAME -and $_.targetExecutions -eq $CHALLENGE_TARGET }
    if ($found) {
        Add-ReportEntry "VERIFY ACHIEVEMENT" "PASS" $resp.StatusCode "goalName=$CHALLENGE_BLOCK_NAME"
        Write-Host "[OK] Achievement found: goalName=$($found.goalName) target=$($found.targetExecutions)" -ForegroundColor Green
    } else {
        Add-ReportEntry "VERIFY ACHIEVEMENT" "FAIL" $resp.StatusCode "Achievement not found in list"
        Write-Host "[FAIL] Achievement not found in response" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "VERIFY ACHIEVEMENT" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 4f: Verify completed block is hidden
Write-Host ""
Write-Host "PHASE 4f: Verify completed challenge block hidden from /block/all..." -ForegroundColor Cyan

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/block/all" -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $allBlocks = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    $stillVisible = $allBlocks | Where-Object { $_.name -eq $CHALLENGE_BLOCK_NAME }
    if (-not $stillVisible) {
        Add-ReportEntry "VERIFY BLOCK HIDDEN" "PASS" $resp.StatusCode "$CHALLENGE_BLOCK_NAME not in list"
        Write-Host "[OK] Completed block correctly hidden from block list" -ForegroundColor Green
    } else {
        Add-ReportEntry "VERIFY BLOCK HIDDEN" "FAIL" $resp.StatusCode "$CHALLENGE_BLOCK_NAME still visible"
        Write-Host "[FAIL] Completed block should not appear in block list" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "VERIFY BLOCK HIDDEN" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
}

# ==================== END CHALLENGE BLOCK PHASES ====================

# Phase 5: Verify Block
Write-Host ""
Write-Host "PHASE 5: Verify block..." -ForegroundColor Cyan

$allBlocksUri = "$BaseUrl/block/all"
try {
    $resp = Invoke-WebRequest -Uri $allBlocksUri -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $blocks = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    $testBlock = $blocks | Where-Object { $_.name -eq $BLOCK_NAME }
    if ($testBlock -and $testBlock.lastExecution) {
        Add-ReportEntry "VERIFY BLOCK" "PASS" $resp.StatusCode "lastExecution=$($testBlock.lastExecution)"
        Write-Host "[OK] Block found with execution date: $($testBlock.lastExecution)" -ForegroundColor Green
    } else {
        Add-ReportEntry "VERIFY BLOCK" "FAIL" $resp.StatusCode "Block not found or no execution date"
        Write-Host "[FAIL] Block verification failed" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "VERIFY BLOCK" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Block retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 6: Check Trends
Write-Host ""
Write-Host "PHASE 6: Check trends..." -ForegroundColor Cyan

$trendUri = "$BaseUrl/trend/countPerDay"
try {
    $resp = Invoke-WebRequest -Uri $trendUri -Method GET `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $trendData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if ($trendData.data -and $trendData.yValues -and $trendData.yValues.Count -gt 0) {
        $yValueCount = $trendData.yValues.Count
        Add-ReportEntry "TRENDS" "PASS" $resp.StatusCode "$yValueCount yValues, data present"
        Write-Host "[OK] Trend data looks good ($yValueCount yValues)" -ForegroundColor Green
    } else {
        Add-ReportEntry "TRENDS" "FAIL" $resp.StatusCode "data or yValues missing/empty"
        Write-Host "[FAIL] Trend data missing or empty" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "TRENDS" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] Trend retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 7: Cleanup - Delete User
Write-Host ""
Write-Host "PHASE 7: Cleanup - Delete user..." -ForegroundColor Cyan

$deleteUserUri = "$BaseUrl/user/me"
try {
    $resp = Invoke-WebRequest -Uri $deleteUserUri -Method DELETE `
        -WebSession $webSession -UseBasicParsing -ErrorAction Stop
    $respData = ConvertFrom-Json (ConvertResponseToString $resp.Content)
    if (-not $respData.hasError) {
        Add-ReportEntry "CLEANUP" "PASS" $resp.StatusCode "{hasError:false}"
        Write-Host "[OK] User deleted (cascade delete of blocks and history)" -ForegroundColor Green
    } else {
        Add-ReportEntry "CLEANUP" "FAIL" $resp.StatusCode "hasError=true"
        Write-Host "[FAIL] Cleanup returned error" -ForegroundColor Red
    }
} catch {
    Add-ReportEntry "CLEANUP" "FAIL" "ERR" $_.Exception.Message
    Write-Host "[FAIL] User deletion failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Phase 8: Stop App
Write-Host ""
Write-Host "PHASE 8: Stop app..." -ForegroundColor Cyan

if ($script:appJobId) {
    try {
        $job = Get-Job -Id $script:appJobId -ErrorAction SilentlyContinue
        if ($job) {
            Stop-Job -Id $script:appJobId -ErrorAction SilentlyContinue
            Remove-Job -Id $script:appJobId -ErrorAction SilentlyContinue
            
            # Kill any lingering mvnw process
            Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
                try { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue } catch {}
            }
            
            Start-Sleep -Seconds 1
            Add-ReportEntry "STOP APP" "PASS" "0" "process terminated"
            Write-Host "[OK] App stopped" -ForegroundColor Green
        }
    }
    catch {
        Add-ReportEntry "STOP APP" "FAIL" "0" $_.Exception.Message
        Write-Host "[FAIL] Failed to stop app: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Print-Report

# Exit with appropriate code
if ($script:overallStatus -eq "PASS") {
    exit 0
}
else {
    exit 1
}
