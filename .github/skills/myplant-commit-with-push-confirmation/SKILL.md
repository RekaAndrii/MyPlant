---
name: myplant-commit-with-push-confirmation
description: "Use this skill when the user asks to commit changes, create a git commit, save current work to git, or publish changes. Trigger phrases: commit changes, create commit, git commit, save to git, push changes, commit and push."
---

# MyPlant Commit With Push Confirmation

## Goal
Commit the current repository changes, then push immediately only when the user explicitly asked for push in the same request.

## Default Flow
1. Check pending changes with `git status --short`.
2. Stage the intended files.
3. Create the commit with a clear message.
4. If the user request explicitly includes push, treat that as confirmation and push.
5. If the user asked only to commit, ask whether to push after commit succeeds.

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
- State what was committed.
- After commit, ask one direct question about push only when the original request did not already include push.