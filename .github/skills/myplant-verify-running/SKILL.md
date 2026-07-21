---
name: myplant-verify-running
description: "Use this skill when the user asks to verify MyPlant is running, check service status, confirm app availability, validate health endpoint, or troubleshoot startup status. Trigger phrases: verify app is running, is app up, check app status, check service health, health check, ping app, verify MyPlant, app availability."
---

# MyPlant Verify Running

## Goal
Quickly verify whether the MyPlant app is running and reachable.

## Default Verify Steps
Run from project root and use port 8080 unless PORT is set.

### Windows PowerShell
1. Health check (Actuator):
   - `Invoke-WebRequest http://localhost:8080/actuator/health -UseBasicParsing`
2. Liveness check:
   - `Invoke-WebRequest http://localhost:8080/actuator/health/liveness -UseBasicParsing`
3. App page check:
   - `Invoke-WebRequest http://localhost:8080/home -UseBasicParsing`

### Unix-like shells
1. Health check (Actuator):
   - `curl -fsS http://localhost:8080/actuator/health`
2. Liveness check:
   - `curl -fsS http://localhost:8080/actuator/health/liveness`
3. App page check:
   - `curl -fsS -o /dev/null http://localhost:8080/home`

## If App Is Not Running
Start app:
- Windows: `mvnw.cmd spring-boot:run`
- Unix-like: `./mvnw spring-boot:run`

Then re-run the health check.

## Output Style
- Keep answer short.
- Show one verify command first.
- Add only one fallback command when verification fails.
