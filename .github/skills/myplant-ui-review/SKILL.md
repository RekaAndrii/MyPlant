---
name: myplant-ui-review
description: "Use this skill when the user asks to review UI code, review frontend, check blocks.js, review index.html, or run a frontend review. Trigger phrases: review ui, review frontend, check ui, ui review, frontend review, review blocks.js, review index.html."
---

# MyPlant UI Review

## Goal

Read `blocks.js`, `index.html`, and `blocks.css` in full and produce a structured review against the checklist below. Report every issue found with the exact file, line reference, and a short fix description. If everything passes, say so explicitly per category.

## Files to read

1. `src/main/resources/static/js/blocks.js`
2. `src/main/resources/templates/index.html`
3. `src/main/resources/static/style/blocks.css`

Read all three files before starting the review.

## Review checklist

### 1. jQuery selectors
- Use `$(this).children("span")` (direct child) not `$(this).find("span")` (deep search) when the tile contains nested elements that share the same tag.
- Selectors targeting elements by class (`.block-item`, `.block-edit-icons`, etc.) must not accidentally match newly added children with the same tag or class.
- Selectors for block-execution must exclude non-block tiles with `.not("#addBlockBtn")` or equivalent.

### 2. Click handlers and event propagation
- Buttons nested inside a `.block-item` tile (edit icons, delete confirm) must call `e.stopPropagation()` to prevent the tile's own click handler from firing.
- Every new block-level click handler must start with `if (editModeActive) return;` so edit mode guards execute flow.

### 3. Edit mode guard
- Any clickable element that should only work in normal mode must check `editModeActive` at the top of its handler.
- Any element that should only work in edit mode must also guard on `editModeActive`.

### 4. DOM assumptions
- JS that reads a block name must use `data-block-name` attribute, not inner text, when the tile contains other text nodes (e.g. nested spans from `.block-delete-confirm`).
- Verify that `data-block-name` is set via `th:attr="data-block-name=${bl.name}"` on every block tile produced by `th:each`.

### 5. Modal hygiene
- Every modal must have: a `resetXxxForm()` function, a Cancel button that calls reset, a backdrop-click handler (`$(e.target).is("#modalId")`), and focus set on the primary input when opened.
- `resetXxxForm()` must clear **all** fields: checkboxes unchecked, conditional divs hidden, text inputs emptied.

### 6. CSS positioning
- Elements added inside `.block-item` (`position:relative`) that use `position:absolute` must have correct `z-index`: above tile content, below modals (i.e. `z-index < 1050`).
- Flex children of `.block-item` must not break the `align-items:center; justify-content:center` centering of the name span.

### 7. `data-*` attributes
- Every block tile from `th:each` must carry `th:attr="data-block-name=${bl.name}"`.
- Any new tile-type element in the block grid that is not an actual block (e.g. `#addBlockBtn`) must be excluded from block-execution selectors.

### 8. Thymeleaf / server-side
- New attributes driven by server data must use `th:attr` or dedicated `th:` attributes — no string concatenation in `style` or `class` attributes.
- New fragments must follow the Bootstrap 4 grid (`col-xs-*`) and not introduce inline widths that break the mobile layout.

## Output format

```
## UI Review Report

### 1. jQuery selectors     — PASS / ISSUES FOUND
<issue description with file:line and fix>

### 2. Click handlers       — PASS / ISSUES FOUND
...

### Summary
PASS — no issues found
  or
ISSUES FOUND — N items require attention
```

List each issue as:
- **File:line** — description of the problem — suggested fix
