# Pomodoro Timer - Requirements

## Original Request

> Add a Pomodoro timer to MyPlant.

## Scope

- Authenticated users access a dedicated Timer page at `/pomodoro` from the navbar.
- Timer data is user-scoped.

## Core Flow

- Focus session duration is fixed at 25 minutes.
- Break session duration is fixed at 5 minutes.
- Break does not auto-start after focus.
- Next focus does not auto-start after break.
- User may end an active break early.

## Timer Controls

- Required controls: Start focus, Cancel focus, End break early, Pause, Resume.
- Pause rule: if a paused session is not resumed within 5 minutes, it is cancelled.

## Recovery

- Active timer state must survive reload/navigation and resume with correct remaining time.
- Recovery uses browser `localStorage`.

## History and Persistence

- Persist completed and cancelled focus sessions.
- Persist start time, end time, elapsed duration, and `cancelled` flag.
- For cancelled sessions, show elapsed duration and completion percentage.
- History UI is today-first, with older sessions available separately.
- Users can delete sessions from history.

## Tagging

- Sessions support post-session tagging with Tasks and Routine Blocks.
- Tagging supports multiple Tasks and multiple Routine Blocks per session.
- Users can edit tags after session creation.

## Acceptance Criteria

1. Timer page is available at `/pomodoro` in the authenticated navbar.
2. Focus sessions run for 25 minutes.
3. Break sessions run for 5 minutes.
4. Focus-to-break and break-to-focus transitions are manual.
5. End-break-early is supported.
6. Pause and Resume are supported.
7. A paused session auto-cancels after 5 minutes without resume.
8. Active timer state recovers correctly after reload/navigation.
9. Completed and cancelled focus sessions are persisted per user.
10. Cancelled sessions show elapsed duration and percent complete.
11. History prioritizes today and still exposes older sessions.
12. Sessions can be deleted.
13. Sessions can be tagged with Tasks and Routine Blocks after completion/cancellation.
