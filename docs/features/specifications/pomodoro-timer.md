# Pomodoro Timer - Functional Specification

## Overview

The Pomodoro timer is an authenticated, standalone focus tool. It operates independently of blocks and records focus-session outcomes for the signed-in user.

## Data Model

### PomodoroSession document (collection: `pomodoroSession`)

| Field | Type | Description |
|---|---|---|
| `_id` | `String` | MongoDB ObjectId; not serialized to JSON |
| `userName` | `String` | Owner and tenant scope |
| `startedAt` | `LocalDateTime` | Start of the focus attempt |
| `endedAt` | `LocalDateTime` | End of the focus attempt |
| `elapsedSeconds` | `long` | Server-derived elapsed focus duration |
| `cancelled` | `boolean` | `false` for a completed 25-minute session; `true` for an early cancellation |

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/pomodoro` | Renders the Timer page |
| `GET` | `/pomodoro/sessions` | Returns the signed-in user's focus history, newest first |
| `POST` | `/pomodoro/sessions` | Saves a completed or cancelled focus attempt |

### POST /pomodoro/sessions

**Request**
```json
{
  "startedAt": "2026-08-07T08:00:00Z",
  "endedAt": "2026-08-07T08:25:00Z",
  "cancelled": false
}
```

**Validation**

- Both timestamps are required.
- A completed session must be exactly 1,500 seconds.
- A cancelled session must be at least 0 and less than 1,500 seconds.
- The server derives `elapsedSeconds` and the authenticated username; client-supplied ownership and duration are never accepted.

## Client State Machine

```text
Idle --start focus--> Focus --25 minutes--> Break --5 minutes--> Idle
                      |                         |
                      +--cancel & persist------>+--end early--> Idle
```

- `Focus` stores its start time and phase end timestamp in `localStorage`.
- On every render and page load, remaining time is calculated from the end timestamp rather than from an in-memory counter.
- Completing focus persists a completed session and starts the break.
- Cancelling focus persists a cancelled session with the actual elapsed duration.
- Breaks are not persisted as focus-session history.
