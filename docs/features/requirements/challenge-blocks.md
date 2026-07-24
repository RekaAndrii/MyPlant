# Challenge Blocks — Requirements

## Original User Request

> I want to add possibility to blocks to show preplanned executions remaining counter in order to define what is amount of executions that have to lead to some change. Basically when I am creating new block, I also specify if it's a challenge — if it is I then need to specify the amount of executions I need to make in order to achieve the goal. When the counter will drop to 0 I will store the information about the name of the goal and expected executions in a separate page called achievements. As to the behavior of the block with remaining executions — once user clicks on it it will change color as usual, but also instead of name of the block user will temporarily for 2 sec see a remaining number in bigger text size.

## User Stories

**US-1: Create a challenge block**
As a user, when I create a new block I can optionally mark it as a challenge by checking a checkbox and entering a target execution count. Non-challenge blocks are unaffected.

**US-2: Remaining executions counter**
As a user, each time I click a challenge block the remaining count decrements by 1.

**US-3: Post-click flash**
As a user, after clicking a challenge block I temporarily see the remaining count (or "Done") in large text for 2 seconds instead of the block name.

**US-4: Challenge completion**
As a user, when the remaining count reaches 0 after a click:
- The block flashes "Done" for 2 seconds.
- The block is removed from the home page (soft-deleted).
- An achievement record is stored.

**US-5: Achievements page**
As a user, I can navigate to the Trophy page from the navbar to see all my completed challenges (goal name, target, date achieved), newest first.

## Agreed Decisions

| Question | Decision |
|---|---|
| Counter at 0 + click | Flash "Done" 2s, soft-delete block, remove from list |
| Block after completion | Soft delete (`completed=true`), hidden from home |
| Achievements page location | `/achievements`, "Trophy" link in navbar next to Trends |
| Counter visibility | Click-only flash — not shown at rest |
| Target editable after creation | No — fixed at creation time |
