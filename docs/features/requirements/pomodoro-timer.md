# Pomodoro Timer - Requirements

## Original User Request

> Add a Pomodoro timer to MyPlant.

## User Stories

**US-1: Open a dedicated timer**
As a signed-in user, I can open a Timer page from the navbar without changing the existing Home block workflow.

**US-2: Run a focus session**
As a user, I can start a fixed 25-minute focus timer and see its remaining time.

**US-3: Take or skip a break**
As a user, after a completed focus session I automatically begin a fixed 5-minute break and may end that break early.

**US-4: Resume an active timer**
As a user, an active focus or break timer resumes with the correct remaining time after I reload the page or navigate back to it.

**US-5: Keep focus history**
As a user, completed focus sessions are saved with their start time, end time, and duration. If I cancel a focus session early, its actual timing is saved and it is marked cancelled.

## Agreed Decisions

| Question | Decision |
|---|---|
| Timer location | Dedicated `/pomodoro` Timer page in the authenticated navbar |
| Cycle | Fixed 25-minute focus followed by fixed 5-minute break |
| Break cancellation | User may end an active break early |
| Focus cancellation | Save the actual elapsed focus attempt with `cancelled=true` |
| Completed-session storage | Persist per-user history with start/end times and duration |
| Active-timer recovery | Store active phase and end timestamp in browser `localStorage` |
| Block integration | None: timer sessions do not execute blocks or affect scores, colors, challenges, goals, or block history |
