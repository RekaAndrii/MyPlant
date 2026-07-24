# Challenge Blocks — Functional Specification

## Overview

Challenge blocks extend the existing block concept with a goal-tracking counter. A block can optionally be created as a "challenge" with a fixed target execution count. Each execution decrements the counter. When it reaches zero an achievement is recorded and the block is soft-deleted from the home page.

---

## Data Model

### Block document (collection: `block`) — new fields

| Field | Type | Description |
|---|---|---|
| `isChallenge` | `boolean` | `true` if this block is a challenge. Default: `false`. |
| `targetExecutions` | `Integer` | Total executions required to complete the challenge. Set at creation, never modified. `null` for non-challenge blocks. |
| `remainingExecutions` | `Integer` | Executions still needed. Initialized to `targetExecutions` on creation, decremented on each execute. `null` for non-challenge blocks. |
| `completed` | `boolean` | `true` once `remainingExecutions` reaches 0. Soft-delete flag — block is excluded from home page queries. Default: `false`. |

### Achievement document (collection: `achievement`) — new

| Field | Type | Description |
|---|---|---|
| `_id` | `String` | MongoDB ObjectId (auto-generated). Not serialized to JSON. |
| `userName` | `String` | Owner of the achievement (multi-tenant key). |
| `goalName` | `String` | Copied from `block.name` at completion time. |
| `targetExecutions` | `int` | Copied from `block.targetExecutions` at completion time. |
| `achievedDate` | `LocalDate` | Date of the final execution (UTC+3 adjusted). |

---

## API Contracts

### POST /block/ — Create block (updated)

**Request body:**
```json
{
  "name": "My Challenge",
  "isChallenge": true,
  "targetExecutions": 30
}
```

**Behavior:**
- If `isChallenge=true` and `targetExecutions` is set, server initializes `remainingExecutions = targetExecutions`.
- Non-challenge fields (`isChallenge` omitted or `false`): block created as before.

**Response:** `{"hasError": false, "message": "ok"}`

---

### GET /block/execute?name={name} — Execute block (updated)

**Response (non-challenge block):**
```json
{
  "hasError": false,
  "message": "ok",
  "isChallenge": false,
  "remainingExecutions": null,
  "completed": false
}
```

**Response (challenge block, in progress):**
```json
{
  "hasError": false,
  "message": "ok",
  "isChallenge": true,
  "remainingExecutions": 14,
  "completed": false
}
```

**Response (challenge block, just completed):**
```json
{
  "hasError": false,
  "message": "ok",
  "isChallenge": true,
  "remainingExecutions": 0,
  "completed": true
}
```

---

### GET /achievements — Trophy page

Returns the `achievements` Thymeleaf view, populated with `List<Achievement>` sorted newest first.

### GET /achievements/all — Achievements JSON endpoint

Returns `List<Achievement>` as JSON, sorted by `achievedDate` descending, scoped to current user.

---

## Behavior Rules

### Block creation
- `isChallenge` defaults to `false` if not sent.
- `remainingExecutions` is server-initialized; client must not send it.
- `targetExecutions` must be `>= 1` if `isChallenge=true` (validation is UI-side for now).

### Block execution
1. `lastExecution` is always updated (UTC+3 adjusted).
2. If `isChallenge=true` and `remainingExecutions != null`:
   - Decrement `remainingExecutions` by 1.
   - If `remainingExecutions <= 0`: set to `0`, set `completed=true`, save `Achievement`.
3. Block is saved.
4. History item is recorded.

### Soft delete
- Completed blocks (`completed=true`) are excluded from `GET /block/all` and the home page.
- The block document remains in MongoDB for audit.

### Cascade delete
When `DELETE /user/me` is called, achievements are deleted along with blocks, history, and the user record.

---

## UI Behavior

### Add block form
- "This is a challenge" checkbox — hidden by default.
- "Target executions" number input — shown only when checkbox is checked.
- On submit: `targetExecutions` is validated `>= 1` client-side before POST.

### Block click — challenge flash sequence

```
1. Strip color classes (red/yellow/green)
2. Call GET /block/execute?name=...
3. On success, if isChallenge=true:
   a. If completed=true:
      - Replace span text with "Done"
      - Apply block-flash-text CSS class (3em font)
      - Add "green" class
      - After 2000ms: fade out and remove .block-col from DOM
   b. If completed=false:
      - Replace span text with remainingExecutions number
      - Apply block-flash-text CSS class
      - Add "green" class
      - After 2000ms: remove block-flash-text, restore original block name
4. If isChallenge=false: add "green" class (existing behavior)
```

### Trophy navbar link
Added to navbar in `index.html` and `achievements.html` as `<li><a href="/achievements">Trophy</a></li>`, positioned after the Trends link.
