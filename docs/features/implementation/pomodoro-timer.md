# Pomodoro Timer - Implementation Notes

## Files Added

| File | Purpose |
|---|---|
| `model/PomodoroSession.java` | MongoDB document for completed and cancelled focus attempts |
| `util/dto/PomodoroSessionRequest.java` | Timestamp-only write request; no client-owned user or duration fields |
| `service/PomodoroSessionService.java` | Session persistence contract |
| `service/impl/PomodoroSessionServiceImpl.java` | User-scoped, newest-first MongoTemplate implementation |
| `controller/PomodoroController.java` | Timer page and focus-history REST endpoints |
| `templates/pomodoro.html` | Dedicated Timer page |
| `static/js/pomodoro.js` | Fixed focus/break state machine and localStorage recovery |
| `requirements/pomodoro-timer.md` | Agreed product requirements |
| `specifications/pomodoro-timer.md` | API, data, and state-machine contract |

## Files Modified

| File | Change |
|---|---|
| `templates/fragments/navbar.html` | Adds the Timer navigation item and active page state |
| `static/style/blocks.css` | Adds scoped Timer page and session-history styles |
| `service/impl/UserServiceImpl.java` | Cascades user deletion to Pomodoro sessions |

## Persistence Rules

- A normal focus record is accepted only when its elapsed duration is exactly 1,500 seconds.
- A cancelled focus record must have an elapsed duration from 0 through 1,499 seconds.
- The controller derives the duration and owner from request timestamps and Spring Security.
- Breaks have no persistence record.
