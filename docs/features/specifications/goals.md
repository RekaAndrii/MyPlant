# Goals — Functional Specification

## Overview

The Goals feature replaces the Achievements page with a structured goal-tracking system. A user defines named Goals, each containing an ordered list of Steps. Steps can be linked to one or more Blocks (challenges); completing a linked challenge auto-marks the step as done. Goals and steps can also be manually toggled done. Completed challenges that are not linked to any goal remain visible in a secondary "Completed Challenges (no goal)" section on the same page.

---

## Data Model

### Goal document (collection: `goal`)

| Field | Type | Description |
|---|---|---|
| `_id` | `String` | MongoDB ObjectId (auto-generated). Not serialized to JSON. |
| `userName` | `String` | Owner (multi-tenant key). |
| `name` | `String` | Display name of the goal. |
| `done` | `boolean` | Manually toggled by user. Default: `false`. |
| `createdDate` | `LocalDate` | Set at creation time. Used for sorting (ASC). |

### GoalStep document (collection: `goalStep`)

| Field | Type | Description |
|---|---|---|
| `_id` | `String` | MongoDB ObjectId (auto-generated). Not serialized to JSON. |
| `userName` | `String` | Owner (multi-tenant key). |
| `goalId` | `String` | References `Goal._id`. |
| `name` | `String` | Step description. |
| `linkedBlockNames` | `List<String>` | Optional. Names of Blocks this step tracks. When any linked challenge completes, the step is auto-marked done. |
| `done` | `boolean` | `true` when manually or auto-completed. Default: `false`. |
| `order` | `int` | 0-based position within the goal. Used for display ordering (ASC). |

### Achievement document (collection: `achievement`) — updated field

| Field | Type | Description |
|---|---|---|
| `goalStepId` | `String` | Optional. Reserved for future explicit linking. Currently `null` on all documents. Achievements where this field is `null` or missing appear in the "no goal" table. |

---

## API Contracts

### GET /goals — Goals page

Returns the `goals` Thymeleaf view with the following model objects:

| Model key | Type | Description |
|---|---|---|
| `goals` | `List<Goal>` | All goals for user, sorted by `createdDate` ASC. |
| `stepsByGoalId` | `Map<String, List<GoalStep>>` | Steps grouped by `Goal._id`, each list sorted by `order` ASC. |
| `unlinkedAchievements` | `List<Achievement>` | Achievements where `goalStepId` is `null` or missing, sorted by `achievedDate` DESC. |
| `availableBlocks` | `List<String>` | Names of all non-completed blocks for the user. Used to populate the block pill-checkboxes in step modals. |

---

### Goal endpoints

| Method | Path | Request body | Description |
|---|---|---|---|
| `POST` | `/goals/` | `{name}` | Create a new goal. Server sets `createdDate = today`, `done = false`. |
| `PUT` | `/goals/{id}` | `{name}` | Rename goal. |
| `PUT` | `/goals/{id}/done` | `{done: true/false}` | Toggle goal done state. |
| `DELETE` | `/goals/{id}` | — | Delete goal and all its steps. |

All endpoints return `{"hasError": false, "message": "ok"}`.

---

### Step endpoints

| Method | Path | Request body | Description |
|---|---|---|---|
| `POST` | `/goals/{goalId}/steps` | `{name, linkedBlockNames[]}` | Add step to goal. Server assigns `order = max existing order + 1` (or `0` if first). |
| `PUT` | `/goals/steps/{stepId}` | `{name, linkedBlockNames[]}` | Edit step name and/or block links. |
| `PUT` | `/goals/steps/{stepId}/done` | `{done: true/false}` | Toggle step done state. |
| `PUT` | `/goals/steps/{stepId}/move` | `{direction: "up"/"down"}` | Reorder step by swapping `order` with adjacent step. No-op if already at boundary. |
| `DELETE` | `/goals/steps/{stepId}` | — | Delete step. |

All endpoints return `{"hasError": false, "message": "ok"}`.

---

### Legacy redirect

`GET /achievements` redirects to `GET /goals` (HTTP 302). The `/achievements/all` JSON endpoint remains unchanged for backward compatibility.

---

## Behavior Rules

### Goal creation
- `done` defaults to `false`; `createdDate` is set server-side.
- Goals are sorted oldest-first on the page.

### Step ordering
- New steps are appended at the end (`order = max + 1`).
- Move up/down swaps `order` values with the adjacent step.
- First step: ↑ button is disabled. Last step: ↓ button is disabled.

### Auto-complete step on challenge completion
When `GET /block/execute` completes a challenge (`remainingExecutions` reaches 0):
1. An `Achievement` is saved (existing behaviour).
2. `GoalStepService.markDoneByBlockName(blockName, userName)` is called.
3. All `GoalStep` documents for the user where `linkedBlockNames` contains `blockName` and `done = false` are set to `done = true`.

### Goal done state
- Done state is manual only — completing all steps does **not** auto-complete the goal.
- A done goal is visually dimmed with strikethrough on its name.

### Cascade delete
When `DELETE /user/me` is called, `GoalStep` and `Goal` documents are deleted along with blocks, history, achievements, and the user record.

### Unlinked achievements
Achievements where `goalStepId` is `null` or missing are shown in a separate card list below the goals. This covers all pre-existing achievements and any future achievements from challenges not linked to a step.

---

## UI Behaviour

### Goals page layout

```
[+ Add Goal]                                    ← top-right

┌── GOAL: "Learn Spanish" [toggle] [✎] [✕] ──────────────┐
│  [toggle] Step 1: "Duolingo streak"  [block-badge] [↑][↓][✎][✕] │
│  [toggle] Step 2: "Finish course"                [↑][↓][✎][✕] │
│  [+ Add Step]                                              │
└────────────────────────────────────────────────────────────┘

════ Completed Challenges (no goal) ════════════════════════
  ★ "100 Push-ups"   Target: 100   2025-01-12
```

### Toggle switches
- Goals use a 36×20 px pill toggle with a sliding thumb. No text label.
- Steps use a compact 30×17 px pill toggle with no label.
- Both animate via CSS transition (`0.2s`). Active (done) state is green (`#648637`).

### Block link selection (step modals)
- Displayed as pill checkboxes (`.step-block-pill`) — one pill per available block.
- Multiple blocks can be selected simultaneously.
- Pre-checked on edit-step open based on current `linkedBlockNames`.
- Pills scroll vertically if the block count exceeds the container height (max-height: 140px).

### Delete goal confirmation
- An absolute-positioned overlay (`z-index: 20`) covers the entire goal card.
- Requires explicit "Yes" click. "No" or any other action dismisses it.

### Modals
All four modals (Add Goal, Edit Goal, Add Step, Edit Step) follow the standard modal hygiene pattern:
- `reset*Form()` clears all fields and unchecks all pills.
- Cancel button and backdrop click both call reset and close.
- Primary input receives focus on open.

### Completed challenge cards
Each unlinked achievement renders as an `.achievement-card` with:
- Green left accent border (`border-left: 3px solid #648637`).
- Gold star icon `★`.
- Challenge name (bold), target executions pill (green), date (grey).

---

## Navigation

- The **Trophy** navbar link is renamed to **Goals** and points to `/goals`.
- Active page key: `'goals'` (replaces `'achievements'` in `navbar(activePage)`).
