---
name: myplant-run-and-token-saver
description: "Use this skill when the user asks how to run the MyPlant app, start the service locally, verify the app is up, check actuator ping health, or asks for shorter/low-token responses. Trigger phrases: run app, start app, start service, launch MyPlant, health check, ping service, reduce tokens, low token output, concise output."
---

# MyPlant Run And Token Saver

## Goal
Provide exact run commands for this repository and keep responses concise to reduce token usage.

## Run Commands
Use these commands from the project root.

### Windows
- Build and run tests: `mvnw.cmd test`
- Start app: `mvnw.cmd spring-boot:run`
- Package jar: `mvnw.cmd clean package`
- Run jar: `java -jar target/MyPlant-0.0.1-SNAPSHOT.jar`

### Unix-like shells
- Build and run tests: `./mvnw test`
- Start app: `./mvnw spring-boot:run`
- Package jar: `./mvnw clean package`
- Run jar: `java -jar target/MyPlant-0.0.1-SNAPSHOT.jar`

## Health And Ping
Default port is `8080` unless `PORT` is set.

- Home check: `http://localhost:8080/home`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator liveness: `http://localhost:8080/actuator/health/liveness`

## Token Saving Behavior
When this skill is invoked:
- Prefer short answers with only required steps.
- Avoid long explanations unless requested.
- Use single command blocks instead of multiple alternatives when possible.
- Summarize outputs in 1-3 lines unless user asks for full logs.
- Ask at most one clarifying question only if blocked.

## Default Quick Response Template
1. Run: one exact command for the user OS.
2. Verify: one health URL.
3. If it fails: one next diagnostic command.
