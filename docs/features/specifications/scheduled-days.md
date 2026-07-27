# Scheduled Days Filter — Functional Specification

## Overview

Blocks can be scheduled for specific weekdays. A block stores an optional list of weekdays (`scheduledDays`). The home page gains a day-filter toggle: when ON, only blocks scheduled for the current weekday (plus blocks with no days configured) are visible. Filtering is performed entirely client-side; the server simply persists and returns the `scheduledDays` list. The toggle's state is remembered in the browser via `localStorage`.

---

## Data Model

### Block document (collection: `block`) — new field

| Field | Type | Description |
|---|---|---|
| `scheduledDays` | `List<Integer>` | Weekdays this block is scheduled for, using `java.time.DayOfWeek` values (Monday = 1 … Sunday = 7). `null` or empty means "always active" (shown every day). Default: `null`. |

Backward compatibility: existing block documents have no `scheduledDays` field. They deserialize to `null`, which the UI treats as "always active", so pre-existing blocks behave exactly as before.

---

## API Contracts

### POST /block/ — Create block (updated)

**Request body:**
```json
{
  "name": "Gym",
  "scheduledDays": [1, 3]
}
```

**Behavior:**
- `scheduledDays` is optional. If omitted or `null`, the block has no schedule (always active).
- Values are integers `1..7` (Mon..Sun). No server-side validation is added (UI-side only), matching the existing challenge-field convention.
- Challenge fields (`isChallenge`, `targetExecutions`) continue to work independently and may be combined with `scheduledDays`.

**Response:** `{"hasError": false, "message": "ok"}`

---

### PUT /block/{name} — Update block (updated)

**Request body:**
```json
{
  "name": "Gym",
  "scheduledDays": [1, 3, 5]
}
```

**Behavior:**
- `scheduledDays` on the existing block is replaced with the value sent in the request (full replacement, not merge).
- Sending `scheduledDays: []` or omitting it clears the schedule (block becomes always active). The UI always sends the current checkbox state.
- Existing name-rename and challenge-recalculation behavior is unchanged.

**Response:** `{"hasError": false, "message": "ok"}`

---

### GET /block/all — List blocks (updated)

Each returned block now includes its `scheduledDays` array (or `null`). No filtering is applied server-side; all non-completed blocks for the user are returned as before.

```json
[
  { "name": "Gym",   "scheduledDays": [1, 3] },
  { "name": "Water", "scheduledDays": null }
]
```

---

## Behavior Rules

### Persistence
- `scheduledDays` flows through `@RequestBody` binding on create and is stored as-is.
- On update, `BlockServiceImpl.update` explicitly copies `scheduledDays` from the incoming block onto the existing document before saving (otherwise it would be dropped, since `update` copies fields individually).

### Home page rendering
- `HomeController` is unchanged — it still returns all non-completed blocks, colored and sorted.
- Each tile is rendered with a `data-scheduled-days` attribute containing the comma-joined day numbers (empty string when none).

---

## UI Behavior

### Day checkboxes (Add and Edit modals)
- Both modals show a row of seven checkboxes labeled Mon, Tue, Wed, Thu, Fri, Sat, Sun.
- Each checkbox carries `data-day="1".."7"` (Monday = 1 … Sunday = 7).
- **Add modal:** all unchecked by default. Checked days are collected into `payload.scheduledDays` on submit; the reset function unchecks all.
- **Edit modal:** all seven always shown; the block's configured days (read from the tile's `data-scheduled-days`) are pre-checked when the modal opens. On save, the current checkbox state is sent as `scheduledDays`.

### Day-filter toggle
- A toggle button is placed under the block grid, near the existing Edit button.
- **Default:** OFF. State is stored in `localStorage` under the key `myplant.dayFilter` (`"on"` / `"off"`), read on page load and re-applied.
- **When ON:** compute today's weekday (JS `Date.getDay()`, converting Sunday `0` → `7`). For each tile (excluding `#addBlockBtn`), read `data-scheduled-days`:
  - Empty / no days → always visible.
  - Contains today's weekday → visible.
  - Otherwise → the tile's `.block-col` wrapper is hidden.
- **When OFF:** all tiles' columns are shown again.
- Toggling never reloads the page. The filter is re-applied after any block add/edit/delete that reloads the page (state is read from `localStorage` on load).

### Interaction with existing behavior
- The filter respects edit mode conventions: toggling the filter is independent of `editModeActive`, but hidden tiles remain hidden while editing.
- The `#addBlockBtn` tile is always excluded from filtering so users can still add blocks with the filter ON.
