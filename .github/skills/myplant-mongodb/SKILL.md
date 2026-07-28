---
name: myplant-mongodb
description: "Use this skill when the user wants to query, inspect, or manipulate the MyPlant MongoDB database directly. Trigger phrases: query mongo, check database, inspect mongo, look at database, mongodb query, check collections, check users in db, find blocks in mongo, count documents, check history, mongo data, database records, what's in mongo, show me data in db."
---

# MyPlant MongoDB Connection

## Overview

The MyPlant app uses MongoDB Atlas. The MCP server (`mongodb-mcp-server`) provides direct database access via tools like `find`, `aggregate`, `insertOne`, `updateOne`, `deleteOne`, etc.

## Connection Details

- **MCP Server name:** `MongoDB` (registered in `~/.claude.json` under the MyPlant project)
- **Connection string:** stored in `MDB_MCP_CONNECTION_STRING` env var (set in the MCP server config)
- **Atlas cluster:** `ac-uzt1wez.jkglvhb.mongodb.net`
- **App database (production):** `MyPlant`
- **Dev database:** `MyPlant-dev` (used when running locally via `.env`)

> Note: The MCP server connects to the **Atlas cluster** directly. The database it operates on
> defaults to what is specified in the connection string (`MyPlant`). To query `MyPlant-dev`,
> pass the `database` parameter explicitly in your tool calls.

## MCP Server Setup

The server is registered project-locally in `~/.claude.json`:

```json
"MongoDB": {
  "type": "stdio",
  "command": "npx",
  "args": ["-y", "mongodb-mcp-server@latest"],
  "env": {
    "MDB_MCP_CONNECTION_STRING": "mongodb+srv://rekaandrii_db_user:...@ac-uzt1wez.jkglvhb.mongodb.net/MyPlant?retryWrites=true&w=majority&appName=MyPlant"
  }
}
```

To verify the server is connected: `claude mcp list`
Expected output: `MongoDB: npx -y mongodb-mcp-server@latest - ✔ Connected`

If not connected, re-add it:
```bash
claude mcp add MongoDB \
  -e MDB_MCP_CONNECTION_STRING="mongodb+srv://rekaandrii_db_user:<password>@ac-uzt1wez.jkglvhb.mongodb.net/MyPlant?retryWrites=true&w=majority&appName=MyPlant" \
  -- npx -y mongodb-mcp-server@latest
```

MCP servers are only loaded at session startup — restart the CLI after adding.

## Collections

| Collection     | Model class       | Description                              |
|----------------|-------------------|------------------------------------------|
| `users`        | `User.java`       | Registered users; field: `userName`      |
| `blocks`       | `Block.java`      | User-owned habit/plant blocks            |
| `history`      | `HistoryItem.java`| Execution log per block per user         |
| `achievements` | `Achievement.java`| Awarded achievements per user            |
| `suggestions`  | `Suggestion.java` | System-level suggestions (no userName)   |

> All collections except `suggestions` are **multi-tenant by `userName`**.
> Every query must include a `userName` filter.

## Common Queries

### List all users
```json
{ "collection": "users", "database": "MyPlant", "filter": {} }
```

### Find blocks for a specific user
```json
{
  "collection": "blocks",
  "database": "MyPlant",
  "filter": { "userName": "alice" }
}
```

### Find blocks never executed (lastExecution is null)
```json
{
  "collection": "blocks",
  "database": "MyPlant",
  "filter": { "userName": "alice", "lastExecution": null }
}
```

### Get execution history for a user
```json
{
  "collection": "history",
  "database": "MyPlant",
  "filter": { "userName": "alice" }
}
```

### Count all documents in a collection
Use the `count` tool or `aggregate` with `$count`.

### Cascade delete a user (manual cleanup)
Run in order:
1. Delete history: `filter: { "userName": "alice" }` on `history`
2. Delete blocks: `filter: { "userName": "alice" }` on `blocks`
3. Delete achievements: `filter: { "userName": "alice" }` on `achievements`
4. Delete user: `filter: { "userName": "alice" }` on `users`

## Key Field Notes

- `userName` — camelCase, used consistently across all models. Never `username` or `user_name`.
- `lastExecution` — `LocalDateTime` stored as ISODate in MongoDB; null means never executed.
- `Block.color` — `@Transient`, never persisted; computed at read-time by `ColorUtil`.
- `HistoryItem.action` — references `HistoryAction` enum (`EXECUTE`, `CREATE`, `DELETE`).
- The app applies `.minusHours(3)` (UTC+3 offset) when writing `lastExecution` in `BlockController`.

## Dev vs Production Database

| Environment | Database name  | Set via                     |
|-------------|----------------|-----------------------------|
| Local dev   | `MyPlant-dev`  | `MONGODB_DATABASE` in `.env`|
| Production  | `MyPlant`      | Atlas connection string      |

When using MCP tools, always pass `"database": "MyPlant-dev"` when inspecting local dev data,
and `"database": "MyPlant"` for production Atlas data.
