# Scheduled Days Filter — Implementation Notes

## File Changes

| File | Type | Description |
|---|---|---|
| `model/Block.java` | Modified | Added `List<Integer> scheduledDays` field with getter/setter; added `import java.util.List` |
| `service/impl/BlockServiceImpl.java` | Modified | `update` now copies `scheduledDays` from the incoming block onto the existing document before save |
| `templates/index.html` | Modified | Added `data-scheduled-days` to each tile; added day-checkbox groups to Add and Edit modals; added `#dayFilterBtn` toggle next to the Edit button |
| `static/js/blocks.js` | Modified | Collect/reset days in Add and Edit forms; pre-check days in edit modal from tile data; new `initDayFilter`/`applyDayFilter` with `localStorage` persistence |
| `static/style/blocks.css` | Modified | Styles for `.scheduled-days*`, `.day-checkbox` (button-style checkboxes), and `#dayFilterBtn` spacing |
| `.github/skills/myplant-e2e-test/e2e-test.ps1` | Modified | 5 new phases: CREATE SCHEDULED, VERIFY SCHEDULED, EDIT SCHEDULED, VERIFY SCHEDULED EDIT, DELETE SCHEDULED |
| `docs/features/requirements/scheduled-days.md` | New | Cleaned user request + user stories + decisions |
| `docs/features/specifications/scheduled-days.md` | New | Full functional spec (data model, API contracts, UI behavior) |
| `docs/features/implementation/scheduled-days.md` | New | This file |

---

## Data Model

`Block.scheduledDays` is a `List<Integer>` of ISO `DayOfWeek` values (Monday = 1 … Sunday = 7).
- `null` / empty ⇒ "always active" (shown every day).
- No custom converter needed — Spring Data Mongo maps `List<Integer>` natively.
- Existing documents lack the field and deserialize to `null`, so they remain always-visible.

---

## Backend

### Create (`POST /block/`)
No controller change required — `scheduledDays` is bound via `@RequestBody` and persisted by `blockService.save(block)`.

### Update (`PUT /block/{name}`)
`BlockServiceImpl.update` copies fields individually onto the existing document, so `scheduledDays` had to be copied explicitly, otherwise it would be lost on every edit:

```java
existing.setName(updated.getName());
existing.setScheduledDays(updated.getScheduledDays());
```

This is a **full replacement**: the client always sends the current checkbox state (including an empty array to clear the schedule).

---

## Frontend

### Tile data attribute (Thymeleaf)
Null-safe comma-join of the day list; empty string when there is no schedule:

```html
<div class="block-item hoverable"
     th:classappend="${bl.color.value}"
     th:attr="data-block-name=${bl.name},
              data-scheduled-days=${bl.scheduledDays != null} ? ${#strings.listJoin(bl.scheduledDays, ',')} : ''">
```

### Collecting days (add + edit)
```javascript
function collectScheduledDays(containerSelector) {
    var days = [];
    $(containerSelector).find("input[type=checkbox][data-day]:checked").each(function() {
        days.push(parseInt($(this).attr("data-day"), 10));
    });
    return days;
}
```
- Add form: `scheduledDays` added to the payload only when at least one day is checked.
- Edit form: `scheduledDays` always sent (possibly empty) so unchecking clears the schedule.

### Edit modal pre-fill
The edit pencil handler reads `data-scheduled-days` from the tile and re-checks the matching boxes — this is the first time the edit modal is populated from real block data:

```javascript
var scheduledDaysRaw = tile.attr("data-scheduled-days");
if (scheduledDaysRaw) {
    $.each(scheduledDaysRaw.split(","), function(i, day) {
        day = $.trim(day);
        if (day) $("#editScheduledDays").find("input[data-day='" + day + "']").prop("checked", true);
    });
}
```

### Day-filter toggle
```javascript
var DAY_FILTER_KEY = "myplant.dayFilter";

function currentIsoWeekday() {          // JS Sun=0..Sat=6  ->  ISO Mon=1..Sun=7
    var d = new Date().getDay();
    return d === 0 ? 7 : d;
}

function applyDayFilter(active) {
    var today = currentIsoWeekday();
    $(".block-item").not("#addBlockBtn").each(function() {
        var col = $(this).closest(".block-col");
        var raw = $(this).attr("data-scheduled-days");
        if (!active || !raw || $.trim(raw) === "") { col.show(); return; }   // OFF or no days => always shown
        var scheduledToday = $.grep(raw.split(","), function(day) {
            return parseInt($.trim(day), 10) === today;
        }).length > 0;
        scheduledToday ? col.show() : col.hide();
    });
}
```

- Toggle state persisted in `localStorage` under `myplant.dayFilter` (`"on"` / `"off"`), restored on page load via `initDayFilter()`.
- `#addBlockBtn` is always excluded so blocks can be added while the filter is ON.
- Because add/edit/delete reload the page, the filter is naturally re-applied from `localStorage` on the next load.

---

## AGENTS.md Frontend Checklist Compliance

- **jQuery selectors:** day collection scoped to modal-specific containers (`#addScheduledDays` / `#editScheduledDays`); filter uses `.block-item` with `.not("#addBlockBtn")`.
- **DOM assumptions:** filtering reads the `data-scheduled-days` attribute, never inner text.
- **`data-*` attributes:** every tile carries `data-scheduled-days` via `th:attr`; no string concatenation in `style`/`class`.
- **Modal hygiene:** both `resetAddBlockForm` and `resetEditForm` now uncheck all day checkboxes.
- **Thymeleaf / server-side:** new attribute driven by server data via `th:attr` with a null-safe conditional.

---

## E2E Test New Phases

| Phase label | Assertion |
|---|---|
| `CREATE SCHEDULED` | `{hasError:false}` on POST with `scheduledDays=[1,3]` |
| `VERIFY SCHEDULED` | `GET /block/all` returns the block with `scheduledDays` containing exactly `1,3` |
| `EDIT SCHEDULED` | `{hasError:false}` on PUT with `scheduledDays=[1,3,5]` |
| `VERIFY SCHEDULED EDIT` | `GET /block/all` returns `scheduledDays` of size 3 containing `5` |
| `DELETE SCHEDULED` | `{hasError:false}` on DELETE (cleanup) |
