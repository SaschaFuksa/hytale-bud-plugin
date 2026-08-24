---
name: docs-sync
description: 'Use this skill to check or bring README.md, CHANGELOG.md and the docs/*.html GitHub Pages site up to date with the actual plugin source — after adding/changing a config key, command, Bud, or Workstation recipe, or whenever asked "is everything current" / "are all configs/commands in the docs".'
---

# Docs Sync (README / CHANGELOG / GitHub Pages)

This project's user-facing documentation lives in three places that must agree with the source and with each other:

1. **`README.md`** — short teaser only (Overview, 3 feature bullets, New Features, and a headline + one-liner + link for Commands/Configuration). It must **never** grow the full reference content back — that's the whole point of the split done in this session. If you're about to add a table or a long list to `README.md`, stop: it almost certainly belongs on a `docs/*.html` page instead, with a link added to `README.md`.
2. **`docs/*.html`** — the detailed reference, one topic per page: `cards.html` (card recipes + summon/dismiss), `work-stations.html` (Workstation recipes, per-role loops, field sizes, tree roots, the full `Work.json` table), `configuration.html` (`LLM`/`Reaction`/`Orchestrator`/`Debug`/`Conversation` config, Bud Registry, prompt management, `versions.yml`), `commands.html` (every `/bud` subcommand). `index.html` only teases each of these with a short section + a link out.
3. **`CHANGELOG.md`** — full version history. `README.md`'s "New Features" section is a short highlight reel of the latest entry, not a duplicate of the whole file.

This is an on-demand checklist, not an automatic check — nothing re-runs it for you on a commit or a save. Invoke it yourself after finishing a feature, or when asked whether the docs are current. (A real always-on check would be a CI job or a git hook, a different and bigger thing than a skill — say so if the user asks for that instead of a manual pass.)

## Step 1 — Pull the ground truth

```
.claude/skills/docs-sync/scripts/ground-truth.sh all
```

This greps the actual source for every config key (`KeyedCodec` entries per `com.bud.core.config.*Config` class), every command's subcommand name and flags/args (`com.bud.app.commands.*`), and every Bud's `workRole`/`restPosition` (`buds/*.yml`). Run a single mode (`configs` / `commands` / `buds`) if you only need one.

This does **not** cover recipes (card/Workstation crafting ingredients) or free-form tables (field sizes, tree-root depths) — those aren't a flat key list the same way. Spot-check those by reading the actual source directly rather than trusting the docs:
- Card recipes: `src/main/resources/Server/Item/Items/Card*.json`, the `Recipe.Input` array.
- Workstation recipes: `src/main/resources/Server/Item/Items/Workstation_*.json`, same field.
- Field sizes: `WorkConfig.getFieldRadius`/`getFieldStructureCount` in `src/main/java/com/bud/core/config/WorkConfig.java`.
- Allowed seeds/fuel per role, tree growth stage seconds: `src/main/resources/work/recipes.yml`.

## Step 2 — Diff against the docs

For each config class in the ground-truth output, check every key appears in `docs/configuration.html` (or, for `WorkConfig`, `docs/work-stations.html#work-config`) with the **current** default — a stale default is as bad as a missing key, so don't just check presence. For each command, check `docs/commands.html` documents every flag/arg, and that any admin-gated one is marked with `<span class="badge-admin">`. For Buds, check the `workRole`s match what `README.md`'s buddy table and `docs/index.html#buddies` claim.

Also check, every time:
- `build.gradle.kts`'s `version` has a matching `## [x.y.z]` entry at the top of `CHANGELOG.md` (see `CLAUDE.md`, "Versioning / changelog" — bump both together, this skill doesn't replace that convention, it catches when it was forgotten).
- `README.md`'s "New Features" bullets reflect the *latest* `CHANGELOG.md` entry, not an older one.
- Nothing in `docs/*.html` references a README section or anchor that no longer exists (`README.md#known-issues` is the only one that should remain — check with `grep -on "README.md#[a-z-]*" docs/*.html` and confirm every match resolves to a real heading).

## Step 3 — Report, then fix

List every gap found as `file — what's missing/stale — where it belongs`. Fix in the right file per the split above: a new config key's full row goes in `docs/configuration.html` (or `work-stations.html` for `WorkConfig`), never in `README.md`. A new command goes in `docs/commands.html`. A new feature worth a teaser sentence goes in `README.md`'s Features/New Features bullets *and* wherever its full detail lives.

After fixing, re-run `ground-truth.sh` once more against the updated docs before calling it done — the same discipline as `.\gradlew build` after a code change, not a one-shot guess.
