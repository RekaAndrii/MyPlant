# Scheduled Days Filter — Requirements

## Original User Request

> I would like to create a new feature. New feature should update the UI with a new toggle that will switch on or off the view. In case it is switched on it will show just the blocks for the day the user specifically configured. The user will have the possibility to specify, per each block, the days it should be configured for. For example, when I create or update a block, in the modal window I would see the list of the days of the week and mark those days I would like to see that block on.
>
> Example: I create a new block "Gym" and specify Monday and Wednesday. Then, if my toggle under the dashboard section is switched on and today is a Tuesday — "Gym" will be hidden. When I click Edit I will always see all days (with the configured ones pre-checked).

## User Stories

**US-1: Configure scheduled days when creating a block**
As a user, when I create a new block I can optionally check any subset of the seven weekdays (Monday–Sunday) to mark which days that block is scheduled for.

**US-2: Day-filter toggle on the dashboard**
As a user, I see a toggle under the block grid. When it is ON, only blocks scheduled for the current weekday are shown; the rest are hidden. When it is OFF, all blocks are shown (existing behavior).

**US-3: Blocks with no days set are always shown**
As a user, if a block has no scheduled days configured (including all pre-existing blocks), it is treated as "always active" and remains visible regardless of the toggle.

**US-4: Edit always shows all days, pre-checked**
As a user, when I open the Edit modal for a block, I always see all seven weekday checkboxes, with the block's currently configured days pre-checked, so I can add or remove days.

## Agreed Decisions

| Question | Decision |
|---|---|
| Block with no days configured | Treated as always active — shown on every day, even when the filter is ON |
| Toggle default state | OFF (show all blocks) |
| Toggle persistence | Remembered in browser `localStorage` (device-local); survives reloads |
| Where filtering happens | Client-side in `blocks.js` — instant toggle, no page reload, `HomeController` unchanged |
| Day ordering / week start | Monday first (Mon–Sun), matching the `Europe/Kiev` locale assumption |
| Storage representation | `scheduledDays` = list of integers `1..7` following `java.time.DayOfWeek` (Monday = 1 … Sunday = 7) |
| Scope | Scheduled days only; challenge behavior is unchanged |
