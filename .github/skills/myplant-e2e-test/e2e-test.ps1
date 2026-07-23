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
            Headers = $Headers
            UseBasicParsing = $true
        }
        
        if ($Method -eq "POST" -or $Method -eq "DELETE" -or $Method -eq "PUT") {
            $params["Headers"]["Content-Type"] = "application/json"
        }
        
        if (-not [string]::IsNullOrWhiteSpace($Body)) {
            $params["Body"] = $Body
        }
        
        $response = Invoke-WebRequest @params
        
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
                Content = $errorResponse.Exception.Response | Out-String
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
$registerBody = "userName=$TEST_USERNAME" + "&email=$TEST_EMAIL" + "&password=$TEST_PASSWORD"

$response = Invoke-AppRequest -Uri $registerUri -Method "POST" -Body $registerBody
if ($response.StatusCode -eq 302) {
    Add-ReportEntry "REGISTER" "PASS" "302" ""
    Write-Host "[OK] User registered" -ForegroundColor Green
}
else {
    Add-ReportEntry "REGISTER" "FAIL" $response.StatusCode "Expected 302 redirect"
    Write-Host "[FAIL] Registration failed (HTTP $($response.StatusCode))" -ForegroundColor Red
}

# Phase 2: Login
Write-Host ""
Write-Host "PHASE 2: Login..." -ForegroundColor Cyan

$loginUri = "$BaseUrl/login"
$loginBody = "username=$TEST_EMAIL" + "&password=$TEST_PASSWORD"

$response = Invoke-AppRequest -Uri $loginUri -Method "POST" -Body $loginBody

$sessionCookie = $null
if ($response.Headers -and $response.Headers['Set-Cookie']) {
    $setCookieHeader = $response.Headers['Set-Cookie']
    if ($setCookieHeader -is [array]) {
        $setCookieHeader = $setCookieHeader[0]
    }
    if ($setCookieHeader -match "JSESSIONID=([^;]+)") {
        $sessionCookie = $matches[1]
    }
}

if ($response.StatusCode -eq 302 -and $sessionCookie) {
    Add-ReportEntry "LOGIN" "PASS" "302" "-> /home"
    Write-Host "[OK] Login successful, JSESSIONID: $sessionCookie" -ForegroundColor Green
}
else {
    Add-ReportEntry "LOGIN" "FAIL" $response.StatusCode "Could not capture session cookie"
    Write-Host "[FAIL] Login failed (HTTP $($response.StatusCode))" -ForegroundColor Red
    $sessionCookie = $null
}

if (-not $sessionCookie) {
    Write-Host ""
    Write-Host "Cannot continue without session. Stopping." -ForegroundColor Red
    Print-Report
    exit 1
}

# Create headers with session cookie for all subsequent requests
$sessionHeaders = @{
    "Cookie" = "JSESSIONID=$sessionCookie"
}

# Phase 3: Create Block
Write-Host ""
Write-Host "PHASE 3: Create block..." -ForegroundColor Cyan

$blockUri = "$BaseUrl/block/"
$blockBody = @{
    name = $BLOCK_NAME
} | ConvertTo-Json

$response = Invoke-AppRequest -Uri $blockUri -Method "POST" -Headers $sessionHeaders -Body $blockBody

if ($response.Success) {
    try {
        $respData = ConvertFrom-Json (ConvertResponseToString $response.Content)
        if (-not $respData.hasError) {
            Add-ReportEntry "CREATE BLOCK" "PASS" $response.StatusCode "{hasError:false}"
            Write-Host "[OK] Block created" -ForegroundColor Green
        }
        else {
            Add-ReportEntry "CREATE BLOCK" "FAIL" $response.StatusCode "hasError=true"
            Write-Host "[FAIL] Block creation returned error" -ForegroundColor Red
        }
    }
    catch {
        Add-ReportEntry "CREATE BLOCK" "FAIL" $response.StatusCode "Invalid JSON response"
        Write-Host "[FAIL] Could not parse response: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Add-ReportEntry "CREATE BLOCK" "FAIL" $response.StatusCode $response.Error
    Write-Host "[FAIL] Block creation failed" -ForegroundColor Red
}

# Phase 4: Execute Block
Write-Host ""
Write-Host "PHASE 4: Execute block..." -ForegroundColor Cyan

$executeUri = "$BaseUrl/block/execute?name=$BLOCK_NAME"
$response = Invoke-AppRequest -Uri $executeUri -Method "GET" -Headers $sessionHeaders

if ($response.Success) {
    try {
        $respData = ConvertFrom-Json (ConvertResponseToString $response.Content)
        if (-not $respData.hasError) {
            Add-ReportEntry "EXECUTE" "PASS" $response.StatusCode "{hasError:false}"
            Write-Host "[OK] Block executed" -ForegroundColor Green
        }
        else {
            Add-ReportEntry "EXECUTE" "FAIL" $response.StatusCode "hasError=true"
            Write-Host "[FAIL] Block execution returned error" -ForegroundColor Red
        }
    }
    catch {
        Add-ReportEntry "EXECUTE" "FAIL" $response.StatusCode "Invalid JSON response"
        Write-Host "[FAIL] Could not parse response: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Add-ReportEntry "EXECUTE" "FAIL" $response.StatusCode $response.Error
    Write-Host "[FAIL] Block execution failed" -ForegroundColor Red
}

# Phase 5: Verify Block
Write-Host ""
Write-Host "PHASE 5: Verify block..." -ForegroundColor Cyan

$allBlocksUri = "$BaseUrl/block/all"
$response = Invoke-AppRequest -Uri $allBlocksUri -Method "GET" -Headers $sessionHeaders

if ($response.Success) {
    try {
        $blocks = ConvertFrom-Json (ConvertResponseToString $response.Content)
        $testBlock = $blocks | Where-Object { $_.name -eq $BLOCK_NAME }
        
        if ($testBlock -and $testBlock.lastExecution) {
            Add-ReportEntry "VERIFY BLOCK" "PASS" $response.StatusCode "lastExecution=$($testBlock.lastExecution)"
            Write-Host "[OK] Block found with execution date: $($testBlock.lastExecution)" -ForegroundColor Green
        }
        else {
            Add-ReportEntry "VERIFY BLOCK" "FAIL" $response.StatusCode "Block not found or no execution date"
            Write-Host "[FAIL] Block verification failed" -ForegroundColor Red
        }
    }
    catch {
        Add-ReportEntry "VERIFY BLOCK" "FAIL" $response.StatusCode "Invalid JSON response"
        Write-Host "[FAIL] Could not parse response: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Add-ReportEntry "VERIFY BLOCK" "FAIL" $response.StatusCode $response.Error
    Write-Host "[FAIL] Block retrieval failed" -ForegroundColor Red
}

# Phase 6: Check Trends
Write-Host ""
Write-Host "PHASE 6: Check trends..." -ForegroundColor Cyan

$trendUri = "$BaseUrl/trend/countPerDay"
$response = Invoke-AppRequest -Uri $trendUri -Method "GET" -Headers $sessionHeaders

if ($response.Success) {
    try {
        $trendData = ConvertFrom-Json (ConvertResponseToString $response.Content)
        
        if ($trendData.data -and $trendData.yValues -and $trendData.yValues.Count -gt 0) {
            $yValueCount = $trendData.yValues.Count
            Add-ReportEntry "TRENDS" "PASS" $response.StatusCode "$yValueCount yValues, data present"
            Write-Host "[OK] Trend data looks good ($yValueCount y-values)" -ForegroundColor Green
        }
        else {
            Add-ReportEntry "TRENDS" "FAIL" $response.StatusCode "data or yValues missing/empty"
            Write-Host "[FAIL] Trend data missing or empty" -ForegroundColor Red
        }
    }
    catch {
        Add-ReportEntry "TRENDS" "FAIL" $response.StatusCode "Invalid JSON response"
        Write-Host "[FAIL] Could not parse trend data: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Add-ReportEntry "TRENDS" "FAIL" $response.StatusCode $response.Error
    Write-Host "[FAIL] Trend retrieval failed" -ForegroundColor Red
}

# Phase 7: Cleanup - Delete User
Write-Host ""
Write-Host "PHASE 7: Cleanup - Delete user..." -ForegroundColor Cyan

$deleteUserUri = "$BaseUrl/user/me"
$response = Invoke-AppRequest -Uri $deleteUserUri -Method "DELETE" -Headers $sessionHeaders

if ($response.Success) {
    try {
        $respData = ConvertFrom-Json (ConvertResponseToString $response.Content)
        if (-not $respData.hasError) {
            Add-ReportEntry "CLEANUP" "PASS" $response.StatusCode "{hasError:false}"
            Write-Host "[OK] User deleted (cascade delete of blocks/history)" -ForegroundColor Green
        }
        else {
            Add-ReportEntry "CLEANUP" "FAIL" $response.StatusCode "hasError=true"
            Write-Host "[FAIL] Cleanup returned error" -ForegroundColor Red
        }
    }
    catch {
        Add-ReportEntry "CLEANUP" "FAIL" $response.StatusCode "Invalid JSON response"
        Write-Host "[FAIL] Could not parse response: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Add-ReportEntry "CLEANUP" "FAIL" $response.StatusCode $response.Error
    Write-Host "[FAIL] User deletion failed" -ForegroundColor Red
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

Print-Report

# Exit with appropriate code
if ($script:overallStatus -eq "PASS") {
    exit 0
}
else {
    exit 1
}
