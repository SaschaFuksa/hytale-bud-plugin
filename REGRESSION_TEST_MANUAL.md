# BUD Plugin – Manual Regression Test

This document describes manual regression tests for the plugin.

## Scope

- Command behavior
- Bud persistence after logout/login
- Bud persistence after unclean server stop (crash simulation)
- LLM reactions (including combat-related reactions)

## Preconditions

- Server starts successfully (`gradlew runServer`)
- Test player can join the world
- `FINE`/`INFO` logs are visible
- LLM provider is configured and reachable

## Quick Smoke Flow (5–10 minutes)

1. Join with a test player.
2. Run `bud create --all`.
3. Run `bud debug --componentData` and verify all expected buds are listed.
4. Run `bud state --defensive`, `--passive`, `--sitting` and verify chat feedback.
5. Run `bud delete --all` and verify all buds are removed.

---

## 1) Command Regression

### 1.1 Create Commands

Test commands:
- `bud create --all`
- `bud create --veri`
- `bud create --keyleth`
- `bud create --gronkh`
- `bud create` (default path)

Expected:
- Correct bud(s) spawn.
- No duplicate spawn when a valid bud of same type already exists.
- Relevant `[BUD]` logs appear.

### 1.2 Delete Commands

Test commands:
- `bud delete --all`
- `bud delete --veri`
- `bud delete --keyleth`
- `bud delete --gronkh`
- `bud delete` (default path)

Expected:
- Selected bud(s) are despawned.
- `PlayerBudComponent` is updated accordingly.
- No server freeze/hang during `--all`.
- Chat/log feedback appears.

### 1.3 State Commands

Test commands:
- `bud state --defensive`
- `bud state --passive`
- `bud state --sitting`

Expected:
- Existing buds switch state.
- Chat message confirms changed state.
- If no bud is available, user receives fallback message (`No role changed.`).

### 1.4 Prompt Commands

Test commands:
- `bud prompt --reload`
- `bud prompt --reset`
- `bud prompt` (default path)

Expected:
- Prompt files are reloaded/reset as intended.
- Chat feedback is shown.
- No exception in logs.

### 1.5 Reset Command

Test command:
- `bud reset`

Expected:
- Existing buds for player are removed.
- Default buds are recreated.
- No duplicate buds remain.

### 1.6 Debug Command

Test command:
- `bud debug --componentData`

Expected:
- Shows current bud IDs/types for the player.
- No NPE when player has no buds.

---

## 2) Persistence Regression

## 2.1 Logout/Login Persistence

Steps:
1. Run `bud create --all`.
2. Verify with `bud debug --componentData`.
3. Logout to menu.
4. Login again with same player.
5. Run `bud debug --componentData`.

Expected:
- Bud ownership and tracked bud types remain consistent after relog.
- No missing/duplicated buds after relog.

## 2.2 Crash Persistence (Unclean Stop)

Steps:
1. Run `bud create --all`.
2. Verify with `bud debug --componentData`.
3. Simulate crash/unclean stop (kill server process, do not run graceful shutdown command).
4. Restart server.
5. Login with same player.
6. Run `bud debug --componentData`.

Expected:
- Persisted data is restored consistently after restart.
- No corrupted state (e.g., tracked bud type exists but NPC missing forever, or duplicate entities).
- No startup exceptions related to BUD components.

Notes:
- If behavior differs between graceful stop and crash stop, document both outcomes.

---

## 3) LLM Reaction Regression

Validate at least these reaction categories:
- Combat
- Block interaction
- Crafting
- Item usage/handling
- Discovery/world events
- Weather/World/State/Mood tracker-driven reactions

For each category run this checklist:
1. Trigger the in-game event.
2. Confirm an LLM call is made (log evidence).
3. Confirm response is generated and attributed to the expected bud.
4. Confirm no blocking/hang on world thread.
5. Confirm fallback behavior on LLM error (error log, no crash).

### 3.1 Combat-specific checks

Scenarios:
- Player enters combat.
- Player exits combat.
- Multiple short combats in sequence.

Expected:
- Combat reaction triggers reliably.
- Reactions are not duplicated excessively.
- No thread/assert errors in logs.

---

## 4) Thread/Store Safety Checks

During all tests, verify logs do **not** contain:
- `Store is currently processing`
- `Assert not in thread! ... but was in VirtualThread`
- `NullPointerException` in command or reaction handlers

---

## 5) Test Report Template

Use this for each run:

- Build/Branch:
- Server start time:
- Tester:
- Result: PASS / FAIL
- Failed test cases:
- Log snippets:
- Notes / follow-ups:

---

## Exit Criteria

Regression is considered passed when:
- All command tests pass
- Both persistence tests pass (logout + crash)
- LLM reaction checks pass for all listed categories
- No critical thread/store/assert errors appear in logs
