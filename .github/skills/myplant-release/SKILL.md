---
name: myplant-release
description: "Use this skill when the user asks to release, ship, publish, merge to master, promote to production, or do a release. Trigger phrases: release, ship it, release to master, merge to master, promote develop, publish release, do a release, release changes."
---

# MyPlant Release Skill

## Goal

Safely release changes from `develop` to `master`:
1. Verify the current branch
2. If not on `develop` — ask the user whether to proceed with the current branch
3. Run the secret-scan safety check
4. Commit any uncommitted changes (if present)
5. Push `develop` to remote
6. Merge `develop` into `master` (fast-forward if possible, merge commit otherwise)
7. Push `master` to remote
8. Switch back to `develop`
9. Report a concise summary

---

## Full Flow

### Step 1 — Check current branch

```powershell
git branch --show-current
```

- If result is `develop` → proceed normally.
- If result is **anything else** → STOP and ask:
  > "You are on branch `<branch>`, not `develop`. Would you like to proceed with releasing from `<branch>` instead?"
  Wait for explicit confirmation before continuing. If the user says no, stop.

### Step 2 — Check for uncommitted changes

```powershell
git status --short
```

- If there are uncommitted changes → run the **Secret-Scan Safety Check** (see below), then commit and push them before merging.
- If the working tree is clean → skip directly to Step 4.

### Step 3 — Secret-Scan Safety Check (MANDATORY if committing)

Before staging anything, scan for secrets:

```powershell
git diff HEAD -- . | grep -iE "(password|passwd|api[_-]?key|access[_-]?key|auth[_-]?token|private[_-]?key|client[_-]?secret|mongodb\+srv://[^:]+:[^@]+@|AKIA[0-9A-Z]{16})" || echo "CLEAN"
```

Rules:
- Match in a non-`.env` file → **STOP**. Report file:line to user. Do not commit.
- `.env` file → never stage it. Verify it is git-ignored:
  ```powershell
  git check-ignore -v .env
  ```
- Matches only in `.gitignore`, `*.md`, or `application.yml` placeholder lines → safe, proceed.
- No matches → proceed.

If clean, stage and commit:

```powershell
git add -A
git commit -m "<descriptive message summarising the pending changes>"
```

### Step 4 — Push the source branch

```powershell
git push origin <source-branch>
```

### Step 5 — Merge into master

```powershell
git checkout master
git pull origin master
git merge <source-branch> --no-ff -m "release: merge <source-branch> into master"
git push origin master
```

Use `--no-ff` to always create a merge commit so the release is visible in the git log.

### Step 6 — Switch back to develop

```powershell
git checkout develop
```

### Step 7 — Report

Print a short summary:
```
Secret scan: CLEAN
Committed:   <yes / no — already clean>
Pushed:      develop -> origin/develop
Merged:      develop -> master
Pushed:      master  -> origin/master
Now on:      develop
```

---

## Safety Rules

- **NEVER force-push** to `master` or `develop`.
- **NEVER skip the secret scan** when there are uncommitted changes.
- **NEVER commit `.env`** — always verify it is git-ignored first.
- If any `git` command fails, stop immediately and report the error to the user. Do not attempt to continue or auto-fix.
- If `master` has commits not in `develop` (diverged), report this to the user and ask how to proceed instead of force-merging.

---

## Windows Git Commands Reference

| Action | Command |
|---|---|
| Current branch | `git branch --show-current` |
| Status | `git status --short` |
| Stage all | `git add -A` |
| Commit | `git commit -m "message"` |
| Push branch | `git push origin <branch>` |
| Checkout | `git checkout <branch>` |
| Pull | `git pull origin <branch>` |
| Merge no-ff | `git merge <branch> --no-ff -m "message"` |
| Switch back | `git checkout develop` |
