# Challenge Blocks — Implementation Notes

## File Changes

| File | Type | Description |
|---|---|---|
| `model/Block.java` | Modified | Added `isChallenge`, `targetExecutions`, `remainingExecutions`, `completed` fields with getters/setters |
| `model/Achievement.java` | New | `@Document(collection="achievement")` with `userName`, `goalName`, `targetExecutions`, `achievedDate` |
| `util/dto/ExecuteBlockResponse.java` | New | Response DTO for `executeBlock` — extends `AjaxResponse` fields with `isChallenge`, `remainingExecutions`, `completed` |
| `service/AchievementService.java` | New | Interface: `save(Achievement)`, `getAll(String userName)` |
| `service/impl/AchievementServiceImpl.java` | New | `MongoTemplate`-backed impl, sorted by `achievedDate DESC` |
| `service/impl/BlockServiceImpl.java` | Modified | `getAllBlocks` excludes `completed=true` via `Criteria.where("completed").ne(true)` |
| `service/impl/UserServiceImpl.java` | Modified | `deleteCurrentUser` now also removes `Achievement` documents for the user |
| `controller/BlockController.java` | Modified | `executeBlock` returns `ExecuteBlockResponse`; injects `AchievementService`; handles challenge decrement and completion; `create` initializes `remainingExecutions` |
| `controller/AchievementsController.java` | New | `GET /achievements` (view), `GET /achievements/all` (JSON) |
| `templates/index.html` | Modified | Trophy link in navbar; challenge checkbox + target input in add-block form |
| `templates/achievements.html` | New | Trophy page with Bootstrap table; empty-state message |
| `static/js/blocks.js` | Modified | Block click handler extended for flash logic; add-block handler extended for challenge form fields |
| `static/style/blocks.css` | Modified | Added `.block-flash-text { font-size: 3em; font-weight: bold; }` |
| `.github/skills/myplant-e2e-test/e2e-test.ps1` | Modified | 6 new phases: CREATE CHALLENGE, EXEC 1/3, EXEC 2/3, EXEC 3/3, VERIFY ACHIEVEMENT, VERIFY BLOCK HIDDEN |
| `docs/features/requirements/challenge-blocks.md` | New | Cleaned user requirement + user stories + decisions |
| `docs/features/specifications/challenge-blocks.md` | New | Full functional spec (data model, API contracts, UI behavior) |
| `docs/features/implementation/challenge-blocks.md` | New | This file |

---

## Key MongoDB Query Patterns

### Exclude soft-deleted blocks
```java
query.addCriteria(new Criteria().andOperator(
    Criteria.where("userName").is(username),
    Criteria.where("completed").ne(true)
));
```
Note: `ne(true)` correctly matches documents where `completed` is `false` **or** the field is absent (existing blocks without the field).

### Fetch achievements sorted newest first
```java
query.addCriteria(Criteria.where("userName").is(userName));
query.with(Sort.by(Sort.Direction.DESC, "achievedDate"));
```

### Cascade delete achievements on user removal
```java
mongoTemplate.remove(new Query(Criteria.where("userName").is(userName)), Achievement.class);
```
Added to `UserServiceImpl.deleteCurrentUser` alongside existing block and history cleanup.

---

## Frontend Flash Logic

```javascript
// On successful execute response:
if (result.isChallenge) {
    if (result.completed) {
        span.addClass("block-flash-text").text("Done");
        block.addClass("green");
        setTimeout(function() {
            block.closest(".block-col").fadeOut(400, function() { $(this).remove(); });
        }, 2000);
    } else {
        span.addClass("block-flash-text").text(result.remainingExecutions);
        block.addClass("green");
        setTimeout(function() {
            span.removeClass("block-flash-text").text(blockName);
        }, 2000);
    }
} else {
    block.addClass("green"); // existing behavior
}
```

- `blockName` is captured before the AJAX call (from `span.text()`), so it can be safely restored after 2 seconds even if the server renames anything.
- `.block-col` is the Bootstrap column wrapper — removing it avoids leaving an empty grid cell.

---

## E2E Test New Phases

| Phase label | Assertion |
|---|---|
| `CREATE CHALLENGE` | `{hasError:false}` on POST with `isChallenge=true, targetExecutions=3` |
| `EXEC CHALLENGE 1/3` | `remainingExecutions=2, completed=false` |
| `EXEC CHALLENGE 2/3` | `remainingExecutions=1, completed=false` |
| `EXEC CHALLENGE 3/3` | `remainingExecutions=0, completed=true` |
| `VERIFY ACHIEVEMENT` | `GET /achievements/all` returns entry with `goalName=E2EChallengeBlock, targetExecutions=3` |
| `VERIFY BLOCK HIDDEN` | `GET /block/all` does not contain `E2EChallengeBlock` |

The existing CLEANUP phase (`DELETE /user/me`) also implicitly verifies cascade deletion of achievements.
