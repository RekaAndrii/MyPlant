---
name: myplant-free-8080
description: "Use this skill when the user asks to free port 8080, kill the process using port 8080, stop the app blocking 8080, or run MyPlant on 8080 instead of switching to 8181. Trigger phrases: free 8080, kill process on 8080, stop port 8080, use 8080, do not use 8181, kill app on 8080."
---

# MyPlant Free 8080

## Goal
Free port 8080 and keep MyPlant running on its default port instead of switching to 8181.

## Default Windows PowerShell Flow
Run from the project root.

1. Find the process listening on port 8080:
   - `Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object LocalAddress, LocalPort, OwningProcess`
2. Show the process details:
   - `Get-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess`
3. Stop that process:
   - `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess -Force`
4. Start MyPlant on 8080:
   - `mvnw.cmd spring-boot:run`

## Fallback When Get-NetTCPConnection Is Unavailable
1. Find the PID:
   - `netstat -ano | findstr :8080`
2. Kill the PID:
   - `taskkill /PID <PID> /F`

## Verification
- Port check: `Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing`
- App check: `Invoke-WebRequest http://localhost:8080/home -UseBasicParsing`

## Response Style
- Prefer port 8080 over alternate ports.
- Kill only the process bound to 8080.
- Keep responses short and command-first.
- Suggest 8181 only if killing the existing process is not allowed or fails.