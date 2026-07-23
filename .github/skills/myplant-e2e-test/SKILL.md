---
name: myplant-e2e-test
description: "Use this skill when the user asks to run the MyPlant E2E test, smoke test, quick end-to-end test, or test the full user flow. Trigger phrases: run e2e test, smoke test MyPlant, test MyPlant, quick e2e, check MyPlant works end to end, end-to-end test, full flow test, test user registration to cleanup."
---

# MyPlant E2E Test

## Goal

Run a complete end-to-end test of the MyPlant application:
1. Register a new test user
2. Login with the test user
3. Create a block
4. Execute the block
5. Verify block execution data
6. Check trends/analytics page
7. Cleanup: delete the test user and all associated data (cascade delete)
8. Generate a concise pass/fail report

The script also **automatically starts the app**, handles port 8080 (kills any process if needed), monitors the startup log in real-time, and stops the app cleanly when done.

## Prerequisites

- MongoDB must be running and reachable at the connection string in `.env`
- `.env` file must exist in the MyPlant project root with `MONGODB_URI` set
- PowerShell 5.0 or later
- Java 21 (for Maven build)
- Maven Wrapper (`mvnw.cmd`) present in project root

## How to Run

From any directory, run:

```powershell
cd C:\Users\AndriiReka\local-github\MyPlant
.\.github\skills\myplant-e2e-test\e2e-test.ps1
```

Or with an optional custom base URL:

```powershell
.\.github\skills\myplant-e2e-test\e2e-test.ps1 -BaseUrl http://localhost:8181
```

## What Each Phase Tests

| Phase | What it tests | Success = |
|---|---|---|
| **START APP** | Kills any process on port 8080, launches Spring Boot app via `mvnw.cmd spring-boot:run`, polls `/actuator/health` until UP | `{"status":"UP"}` within 120 seconds |
| **REGISTER** | `POST /register` with test email, username, password | HTTP 302 redirect |
| **LOGIN** | `POST /login` with email + password, captures `JSESSIONID` cookie | HTTP 302 redirect, session cookie present |
| **CREATE BLOCK** | `POST /block/` with block name, using session cookie | `{hasError:false, message:"ok"}` |
| **EXECUTE** | `GET /block/execute?name=E2ETestBlock` | `{hasError:false, message:"ok"}` |
| **VERIFY BLOCK** | `GET /block/all`, find test block with `lastExecution` set | Block present with non-null execution date |
| **TRENDS** | `GET /trend/countPerDay` returns valid trend data | `TrendDto.data` and `yValues` array non-empty |
| **CLEANUP** | `DELETE /user/me` — deletes user + all blocks + all history (cascade) | `{hasError:false, message:"ok"}` |
| **STOP APP** | Terminate the background app job and any Java processes | Process exits cleanly |

## Sample Report Output

A **PASS** report looks like:

```
========================================
  MyPlant E2E Test  2026-07-23 14:32:01
========================================
PHASE                STATUS     CODE     NOTE
START APP            PASS       200      UP in 34s
REGISTER             PASS       302
LOGIN                PASS       302      -> /home
CREATE BLOCK         PASS       200      {hasError:false}
EXECUTE              PASS       200      {hasError:false}
VERIFY BLOCK         PASS       200      lastExecution=2026-07-23
TRENDS               PASS       200      3 yValues, data present
CLEANUP              PASS       200      {hasError:false}
STOP APP             PASS       0        process terminated
========================================
  OVERALL: PASS
========================================
```

A **FAIL** in one phase will mark the overall as FAIL:

```
...
CREATE BLOCK         FAIL       400      hasError=true
...
========================================
  OVERALL: FAIL
========================================
```

## Interpreting Results

### PASS = Everything works as expected
- All HTTP responses have correct status codes
- JSON responses contain expected fields with valid data
- Cascade delete confirmed

### FAIL = One or more issues detected

**Common failure reasons:**

| Phase | Reason | Fix |
|---|---|---|
| **START APP** | Timeout waiting for app | Check MongoDB connectivity, review startup logs, ensure Java 21 is available |
| **REGISTER** | Already registered | Manual cleanup in MongoDB, or use a different test timestamp |
| **LOGIN** | Wrong credentials or session not captured | Check email exists in MongoDB, verify password |
| **CREATE BLOCK** | `hasError=true` | Check if block name conflicts, verify user is logged in |
| **TRENDS** | Data or yValues empty | May be normal if no history data; not critical for smoke test |
| **CLEANUP** | `hasError=true` | Verify the new `/user/me` endpoint is deployed |

## Reading Startup Logs

The script streams the Spring Boot startup output in real-time (in dark gray). Watch for:

```
Started MyPlantApplication in ... seconds
```

If startup hangs or fails, look for error messages like:

- `Connection refused` → MongoDB not running
- `Port 8080 already in use` → A previous app instance wasn't stopped
- `Could not find or load main class` → Java 21 or Maven issue

## Exit Codes

- **0** = all phases passed (OVERALL: PASS)
- **1** = one or more phases failed (OVERALL: FAIL)

Use the exit code in CI/CD:

```powershell
.\.github\skills\myplant-e2e-test\e2e-test.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "E2E test passed!"
} else {
    Write-Host "E2E test failed!"
    exit 1
}
```

## Reusable Script Design

- **Automatic app lifecycle:** Script starts, monitors, and stops the app for you
- **Session cookie handling:** Captures and reuses JSESSIONID for all requests
- **Cascade delete confirmation:** Verifies user + blocks + history are deleted
- **No external dependencies:** Pure PowerShell, no curl or jq needed
- **Idempotent test user:** Uses timestamp in email/username, safe to run multiple times
- **Real-time feedback:** Streaming app logs + per-phase pass/fail messages

## Troubleshooting

### "Port 8080 already in use"
Run the cleanup manually:
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -First 1 | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force
}
```

### "MongoDB connection failed"
Ensure MongoDB is running:
```powershell
# Check if MongoDB is listening
netstat -ano | findstr :27017
```

### "JSESSIONID not captured"
The login may have failed. Check if the credentials are correct and the user exists.

### "App not starting"
Rebuild and redeploy:
```powershell
cd MyPlant
mvnw.cmd clean package -DskipTests
mvnw.cmd spring-boot:run
```

## Advanced Usage

**Run multiple times to verify consistency:**

```powershell
for ($i = 1; $i -le 5; $i++) {
    Write-Host "=== Run $i ===" -ForegroundColor Cyan
    .\.github\skills\myplant-e2e-test\e2e-test.ps1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Run $i failed!" -ForegroundColor Red
        exit 1
    }
}
Write-Host "All 5 runs passed!" -ForegroundColor Green
```

**Use in CI/CD (GitHub Actions):**

```yaml
- name: Run MyPlant E2E Test
  run: |
    cd MyPlant
    .\.github\skills\myplant-e2e-test\e2e-test.ps1
```

## Notes

- **Test user cleanup:** The script deletes the test user at the end. If the script is interrupted before cleanup, manually delete the user from MongoDB.
- **Hardcoded UTC+3 offset:** The app applies `.minusHours(3)` when recording execution. This is expected behavior.
- **Trends may be empty on first run:** If no historical data exists, the trends endpoint might return empty `yValues` — this is not a critical failure.
