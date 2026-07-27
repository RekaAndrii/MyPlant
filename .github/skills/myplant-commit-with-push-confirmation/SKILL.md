---
name: myplant-commit-with-push-confirmation
description: "Use this skill when the user asks to commit changes, create a git commit, save current work to git, or publish changes. Trigger phrases: commit changes, create commit, git commit, save to git, push changes, commit and push."
---

# MyPlant Commit With Push Confirmation

## Goal
Commit the current repository changes, then push immediately only when the user explicitly asked for push in the same request.

## Default Flow
1. Check pending changes with `git status --short`.
2. **Run the secret-scan safety check (see below) — STOP if any secrets are detected.**
3. Stage the intended files.
4. Create the commit with a clear message.
5. If the user request explicitly includes push, treat that as confirmation and push.
6. If the user asked only to commit, ask whether to push after commit succeeds.

## Secret-Scan Safety Check (MANDATORY before every commit)

Before staging or committing anything, scan the full diff for secrets. Run both commands:

```powershell
# 1. Scan staged + unstaged changes for common secret patterns
git diff HEAD -- . | Select-String -Pattern '(?i)(password|passwd|secret|api[_-]?key|access[_-]?key|auth[_-]?token|private[_-]?key|client[_-]?secret|mongodb(\+srv)?://[^:]+:[^@]+@|AKIA[0-9A-Z]{16})'

# 2. Also check any new/modified files that are NOT .env for hardcoded URI-style credentials
git status --short | Where-Object { $_ -notmatch '\.env' } | ForEach-Object {
    $file = ($_ -split ' ', 2)[1].Trim()
    if (Test-Path $file) { Select-String -Path $file -Pattern 'mongodb(\+srv)?://[^:]+:[^@]+@' }
}
```

### Rules
- **If any match is found in a non-`.env` file** → STOP. Do NOT commit. Tell the user exactly which file:line contains the suspected secret and ask them to remove it before proceeding.
- **`.env` file** → NEVER stage or commit `.env`. Verify it is in `.gitignore` before proceeding:
  ```powershell
  git check-ignore -v .env
  ```
  If `.env` is NOT ignored → warn the user and refuse to commit until they add `.env` to `.gitignore`.
- **Matches only inside `.gitignore`, `*.md` docs, or `application.yml` placeholder lines like `${MONGODB_URI}`** → these are safe; note them but do not block the commit.
- If the scan returns no results → proceed normally.

## Push Rule
- Push only after explicit user confirmation.
- Treat a request that already says `commit and push` as that confirmation.
- If the request says only `commit`, ask before pushing.

## Windows PowerShell Commands
- Check status: `git status --short`
- Stage all: `git add -A`
- Commit: `git commit -m "<message>"`
- Push after confirmation: `git push origin <current-branch>`

## Response Style
- Keep responses short.
- If the secret scan is clean, say so in one line before stating what was committed.
- If a secret is found, stop immediately and report only the file:line — do not commit.
- After a clean commit, ask one direct question about push only when the original request did not already include push.
