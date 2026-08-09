# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Bud Plugin** — a Hytale server mod (Java 25) that adds LLM-powered companion NPCs ("Buds": Veri, Gronkh, Keyleth) who follow the player, fight alongside them, and react to world/combat/chat events with LLM-generated (or fallback) dialogue. Built on the `hytale-mod` Gradle plugin against the Hytale server API.

The mod works fully without an LLM configured (canned fallback messages), but the point of the project is the LLM integration.

## Build & run

```
.\gradlew decompileServer   # one-time: decompiles server API for IDE use, run first on a fresh clone
.\gradlew build              # compile + shadow-jar (JUnit 5 tests run as part of `build`/`test`)
.\gradlew test                # run tests only
.\gradlew test --tests com.bud.SomeTest   # run a single test class
.\gradlew runServer          # launch a local Hytale server with the plugin loaded
.\gradlew runServer --debug-jvm   # same, suspended for remote debug on port 5005 (see .vscode/launch.json "Attach to Server (Debug)")
```

Equivalent VS Code tasks exist ("Gradle: Build", "Gradle: Run Server", "Gradle: Run Server (Debug)"); build always runs before the server tasks.

First run in-game: `/auth login device` then `/auth persistence Encrypted` to persist login.

There are currently no test source files under `src/test/java` — the JUnit 5 (`junit-jupiter`) infrastructure is wired up in `build.gradle.kts` but unused.

### Asset sync

`runServer` is `finalizedBy` a `syncAssets` task that copies anything the game wrote to `build/resources/main` back into `src/main/resources` after the server stops (used because the in-game asset editor can save changes at runtime). `manifest.json` is excluded from this sync since it's a template expanded from `gradle.properties` at build time — never hand-edit the built one.

### Plugin metadata

Plugin id/version/author/description live in [gradle.properties](gradle.properties) and `build.gradle.kts` (`group`, `version`), and get expanded into `manifest.json` by the `processResources` task. Main entrypoint is `com.bud.BudPlugin` (`plugin_main_entrypoint`).

### Reference: local Hytale install mirror (`/reference`)

`reference/` (gitignored) is a **hand-extracted, non-decompiled** mirror of the local Hytale game installation, used for grepping engine internals (ECS, events, codecs) and asset structure that aren't otherwise documented:

- `reference/server/` — the exploded Hytale server distribution: raw compiled `.class` files (no `.java` sources), so it's good for grepping class/method/field/package names but not for reading decompiled logic.
- `reference/assets/` — the raw Hytale asset tree (`Common/`, `Server/`, `Cosmetics/`, texture/model files, `manifest.json`, etc.), mirroring the same layout used under `src/main/resources/`.

Both were copied directly from the local Hytale install at `C:\Users\sasch\AppData\Roaming\Hytale\install\release\package\game\latest` (the `Server` folder and `Assets.zip` respectively).

**Last extracted: 2026-08-08.** This is a snapshot of whatever build happened to be installed locally, not something Gradle regenerates — re-check it roughly every month: compare the `LastWriteTime` of `Server`/`Assets.zip` at that install path against the date above, and re-extract into `reference/server`/`reference/assets` if the local install has moved on, since Hytale internals can change between builds and a stale mirror can be actively misleading. Update this date whenever you re-extract.

This is separate from the `decompileServer` Gradle task (see below), which prepares attached sources for the IDE rather than populating `/reference`.

`jdk-25.0.2/` (also gitignored) is a bundled JDK used as the project's Java toolchain (see `.vscode/settings.json`).

## Architecture

### Plugin bootstrap

[BudPlugin.java](src/main/java/com/bud/BudPlugin.java) is the `JavaPlugin` entrypoint. In `setup()` it:
1. Registers the codec for the card interaction (`CardBud`, generic across all Buds — see "Bud identity/profile system" below).
2. Loads/saves five config sections (`LLM`, `Reaction`, `Orchestrator`, `Conversation`, `Debug` — one JSON file each in the mod's config folder, backed by `Config<T>` + a `CODEC`).
3. Registers the `BudComponent` / `PlayerBudComponent` ECS components.
4. Registers the `/bud` command tree and all ECS systems/event handlers — each gated by its `Reaction` config flag (e.g. combat systems only register if `EnableCombatReactions` is true).

Config classes (`com.bud.core.config.*`) follow one pattern throughout: a private-constructor singleton (`getInstance()`/`setInstance()`) plus a static `BuilderCodec<T> CODEC` built from `KeyedCodec` entries — copy an existing config class when adding a new setting rather than inventing a new pattern.

### The event → cache/tracker → LLM-message → orchestrator pipeline

Nearly every feature under `com.bud.feature.*` (block, item, combat, crafting, discover, teleport, player state, world/weather, bud-to-bud reactions, chat/conversation) follows the same vertical-slice shape; understanding one explains the rest:

1. **Filter system** (`*FilterSystem`, an ECS `EntityEventSystem`) listens for a raw engine event (e.g. `BreakBlockEvent`), resolves the acting player's `PlayerBudComponent`/`BudComponent`, and builds a small immutable `*Entry` record describing what happened (e.g. `BlockEntry`).
2. **Cache/tracker** (`Recent*Cache` extends `AbstractCache`, or `*Tracker` extends `AbstractTracker`) stores recent per-player history and enforces its own enqueue cooldown before anything reaches the LLM.
3. **`LLM*MessageCreation`** (extends `AbstractLLMMessageCreation`) builds the actual prompt: `createPrompt()` delegates to `createLLMPrompt()` when `LLMConfig.isEnableLLM()` is true, else to `createFallbackPrompt()` which returns a canned string from the Bud's `BudMessage`/YAML fallback data — this is what keeps the plugin functional with no LLM configured.
4. The resulting entry + message-creation strategy is wrapped as an `LLMInteractionEntry` (`com.bud.llm.interaction`) and pushed onto the **`Orchestrator`** (`com.bud.feature.queue.orchestrator`) via an `OrchestratorQueue` on one of five `OrchestratorChannel`s (`PLAYER`, `AMBIENT`, `ACTIVITY`, `COMBAT`, `SOCIAL`).
5. The `Orchestrator` singleton runs one scheduled `tick()` (interval from `OrchestratorConfig`) per instance, per-player: it enforces global + per-channel cooldowns, deduplicates same-type events, drops low-priority events once a channel's queue is full, and dispatches at most one event per tick — this is what stops Buds from spamming chat. `PLAYER` channel (direct chat replies) always gets served immediately, bypassing the cooldown gate.
6. Dispatch hands off to `LLMInteractionManager.processInteraction()`, which builds the `Prompt`, optionally augments it with conversation memory (`ConversationMemoryService`), calls `LLMCaller` → `ILLMClient` (only if `EnableLLM`), and finally posts the reply as a `ChatEvent` + plays a `SoundEvent`.

When adding a new reaction type, add the `*Entry`/cache/filter-system/`LLM*MessageCreation` quartet under a new `com.bud.feature.<name>` package, wire the filter system's registration in `BudPlugin.registerEvents()` behind a new `Reaction` config flag, and enqueue through the `Orchestrator` on the appropriate channel rather than calling the LLM directly.

### LLM client layer

`com.bud.llm.client`: `ILLMClient` is implemented by `BudLLMClient` (generic OpenAI-compatible `/v1/chat/completions`, via `AbstractLLMClient`) and `Player2LLMClient` (Player2 API). `LLMClientFactory.createClient()` picks between them based on `LLMConfig.isUsePlayer2API()`. `LLMCaller` is the shared singleton entrypoint that owns one virtual-thread executor for all LLM calls and reuses the appropriate client.

### Bud identity/profile system

Buds are fully data-driven — there is no `BudType` enum or per-Bud Java class. `BudRegistry` (`com.bud.core.registry`, singleton analogous to `LLMPromptManager`) loads one `BudDefinition` per Bud from `buds/<id>.yml` in the mod's runtime data folder (packaged defaults for `veri`/`keyleth`/`gronkh` are copied in on first start, same mechanism as the LLM prompts). `BudDefinition` supplies the NPC's display name, color, NPC type id, weapon/armor ids, pronoun, favorite day, sound set (`BudSoundDefinition`), and `getBudMessage()` (personality/fallback text, loaded via `LLMPromptManager` from the per-bud YAML prompt file keyed by `promptKey`). A Bud's identity everywhere in code is just its lowercase string id (e.g. `"veri"`), normalized via `BudRegistry.normalize()`; `buds/roster.yml`'s `defaultBuds` list (at most 3) is the subset `/bud create` spawns when called without an id — other defined Buds remain summonable individually. `BudComponent` (ECS component on the spawned NPC entity) tracks live per-Bud state (current mood, state, bud id); `PlayerBudComponent` (ECS component on the player entity) tracks that player's owned/spawned Bud ids (`Set<String>`, persisted under the legacy `BudTypes` codec key for wire compatibility with older saves).

### Prompt management

LLM system prompts and personality/fallback text live as YAML under `src/main/resources/prompts/` (`buds/*.yml` per companion + mood text, `interaction/*.yml`, `world/*.yml` incl. per-zone files, `system_prompt.yml`). On first server start these are copied into the mod's runtime folder; `LLMPromptManager` loads/reloads them from there at runtime (never bakes YAML content into Java). `/bud prompt` reloads missing files without a restart; `/bud prompt --reset` overwrites the runtime copies back to the packaged defaults — treat that command as destructive of user customization.

### Commands

`BudCommandCollection` (`com.bud.app`) registers one subcommand class per `/bud <subcommand>` under `com.bud.app.commands`, extending the engine's `AbstractCommandCollection`/subcommand base classes. Follow the existing subcommands (`CreationCommand`, `DeletionCommand`, `StateCommand`, `MemoryCommand`/`MemorySetCommand`/`MemoryDeleteCommand`, `PromptCommand`, `DebugCommand`, `ResetCommand`) as the template for new ones, and see [README.md](README.md) for the full current command surface and flags.

### Memory/conversation system

Under `com.bud.feature.chat.conversation`: `ConversationMemoryService` is the entrypoint used by `LLMInteractionManager` to augment prompts with recent history and persist new memories after each interaction. `RegularMemoryStore` holds decaying, priority-evicted per-player-per-Bud memories (depth/decay/min-importance from `ConversationConfig`); `LegendaryMemoryStore` holds a small number of permanently-retained "legendary" memories per Bud. `DialogModeTracker` manages turn-based back-and-forth chat sessions (idle/active/turn-interval timers). `ConversationMemoryPersistence` handles load/save to disk. Bud-to-bud equivalents live in `com.bud.feature.bud.reaction` (`BudReactionChainTracker` chains a short back-and-forth between two Buds after a reaction fires).

### Threading

Engine ECS callbacks (filter systems) run on the world thread; LLM calls are dispatched onto virtual threads (`Thread.ofVirtual()` in `Orchestrator.dispatch`, and the shared executor in `LLMCaller`) so blocking HTTP calls to the LLM never stall the world tick. `BudManager` has an `executeOnWorldThread` fallback for entity-store queries that must run on the world thread but might be invoked off it.

## Configuration reference

The user-facing config keys (LLM, Reaction, Orchestrator, Debug, Conversation sections) are documented in [README.md](README.md)'s "⚙️ Configuration (LLM)" section — treat that table as the source of truth when adding/renaming a config field, and keep it in sync with the corresponding `com.bud.core.config.*` class.

## Versioning / changelog

Bump `version` in `build.gradle.kts` and add an entry to [CHANGELOG.md](CHANGELOG.md) (Added/Fixed/Performance sections) for user-facing changes; the README's "New in X.Y.Z" section is a shorter highlight reel of the same and links back to the full changelog.
