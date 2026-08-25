# Improvement TODO

Findings from a pass over `src/main/java` looking for bad code quality, redundant code,
dead code, and configs that hold the wrong kind of thing. Grouped by category, ordered
roughly by impact within each group. File:line references point at the code as of this
pass — re-check before fixing since neighboring lines shift.

## Bad Code Quality

### 1. Default LLM config ships what looks like a real API key and a personal LAN IP — kept as-is, intentional

`src/main/java/com/bud/core/config/LLMConfig.java:13-15` ships a real-looking LAN IP,
model id, and credential-shaped API key as the field defaults. Raised and explicitly
confirmed with the project owner: not a live/sensitive credential, and deliberately left
as a concrete example of the shape a value here should take rather than an empty
placeholder. No action needed.

### 2. Exception handling systematically discards stack traces

50 occurrences across 35 files (e.g. `src/main/java/com/bud/core/BudManager.java:433`,
`src/main/java/com/bud/core/registry/BudRegistry.java:131,147,166,178`,
`src/main/java/com/bud/feature/queue/orchestrator/Orchestrator.java:171,278`) follow the
same shape:

```java
} catch (Exception e) {
    LoggerUtil.getLogger().severe(() -> "[BUD] ... " + e.getMessage());
}
```

Only 2 call sites in the whole codebase (`BudManager.java` and `DialogModeTracker.java`,
and even those only for a `Level`-based log call, not a full `log(Level, msg, Throwable)`)
preserve the actual `Throwable`. `e.getMessage()` alone is frequently `null` or
uninformative (e.g. plain `NullPointerException`), and every one of these swallows the
stack trace needed to actually locate the bug in production. Fix: standardize on
`LoggerUtil.getLogger().log(Level.SEVERE, "[BUD] ...", e);` (or equivalent) wherever the
exception itself, not just its message, is worth keeping — which is effectively all of
these `catch (Exception ...)` blocks.

### 3. `BudManager` is a god class mixing unrelated responsibilities

`src/main/java/com/bud/core/BudManager.java` (454 lines) does all of: bud/entity lookup
(`findBudComponent`, `getBudComponent`, `isValidBud`), player tracking
(`registerPlayer`/`unregisterPlayer`/`getTrackedPlayers`), bud-state transition rules
(`getNextState`), cross-thread entity queries (`executeOnWorldThread`,
`collectAllBudComponents`), and ~160 lines of pure spawn-position geometry
(`getSpawnPosition`, `getSpawnPositionInFrontOfPlayer`, `findFreeLateralPosition`,
`candidateAt`, `getPlayerPositionWithOffset`, `getRotationFacingPlayer`, lines 169-330).
None of the geometry code touches `BudManager`'s own state (`trackedPlayers`) — it's all
static-shaped helper math parameterized by `PlayerRef`/`World`. Fix: extract the spawn
positioning block into its own `BudSpawnPositioning` (or similar) class; keep `BudManager`
to entity/component lookup and player tracking.

### 4. Singleton thread-safety is inconsistent across near-identical classes — done

`BudRegistry`, `LLMPromptManager`, and `WorkRecipeConfig` now use
`private static volatile T instance;`, matching the rest of `com.bud.core.config.*`.

### 5. Inconsistent indentation in `LLMCombatMessageCreation` — done

Reformatted to the 4-space convention used by every sibling `LLM*MessageCreation` class.

### 6. "Constants" declared as non-static final instance fields — done

`AbstractQueue.INITIAL_DELAY_MS`/`POLLING_INTERVAL_MS` are now `private static final`.

### 7. `AbstractCache` bakes a config value in at construction time

`src/main/java/com/bud/feature/AbstractCache.java:18`:

```java
private final long enqueueCooldownMs = OrchestratorConfig.getInstance().getOrchestratorChannelCooldownMs();
```

Read once when the owning `Recent*Cache` singleton class is first loaded, then never
again. There's currently no live-reload path for `OrchestratorConfig` so this happens to
be safe today, but it's a silent trap: the value depends on `OrchestratorConfig` already
being populated by `BudPlugin.setupConfig()` at the moment any `Recent*Cache` class is
first *referenced* (JVM class-init order), and it will quietly go stale the day a
`/bud reload` (or similar) for `OrchestratorConfig` is added. Fix: read
`OrchestratorConfig.getInstance().getOrchestratorChannelCooldownMs()` inside
`shouldEnqueue()` instead of caching it in a field.

## Redundant Code

### 1. Five `Recent*Cache` classes copy-paste the same dedup-then-enqueue skeleton

`RecentBlockCache`, `RecentDiscoverCache`, `RecentCraftCache`, `RecentOpponentCache`
(`src/main/java/com/bud/feature/{block,discover,crafting,combat}/Recent*Cache.java`) and,
with an extra twist, `RecentItemCache`
(`src/main/java/com/bud/feature/item/RecentItemCache.java`) all implement `add()` as:
type-check the incoming `IQueueEntry`, `cache.compute()` to append-with-a-dedup-check
against the last entry, cap at `MAX_HISTORY`, log, then `shouldEnqueue()` →
`Orchestrator.getInstance().enqueue(new OrchestratorQueue(...))`. Only the per-field
equality check and the channel/event-type/message-creation-class differ between them.
`RecentItemCache` additionally does unnecessary unchecked raw-type gymnastics
(`(LinkedList<ItemEntry>) (LinkedList<?>) list`, `@SuppressWarnings("unchecked")`,
lines 25-51) to work around this instead of just operating on `LinkedList<IQueueEntry>`
like every other cache does — `ItemEntry` already implements `IQueueEntry`, so the cast is
pure self-inflicted noise.

Fix: move the append/dedup/cap/enqueue skeleton into `AbstractCache` as a template method
(e.g. `protected final void addEntry(String playerName, T entry, BiPredicate<T,T> isDuplicate,
OrchestratorChannel channel, String eventType, AbstractLLMMessageCreation messageCreation)`),
leaving each subclass to supply only its duplicate-check predicate and channel/event-type.
This also removes `RecentItemCache`'s raw-type casting entirely.

### 2. The mood-instruction block is duplicated verbatim across 13 `LLM*MessageCreation` classes

Every one of `LLMBlockMessageCreation`, `LLMCombatMessageCreation`, `LLMCraftMessageCreation`,
`LLMDiscoverMessageCreation`, `LLMItemMessageCreation`, `LLMTeleportMessageCreation`,
`LLMStateMessageCreation`, `LLMPlayerStateMessageCreation`, `LLMWorldMessageCreation`,
`LLMWeatherMessageCreation`, `LLMWorkMessageCreation`, `LLMFavoriteDayMessageCreation`, and
`LLMBudReactionMessageCreation` repeats this same 8-line block inside `createLLMPrompt()`
(example from `src/main/java/com/bud/feature/block/LLMBlockMessageCreation.java:54-61`):

```java
if (!blockEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
    systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
    systemPromptBuilder.append("\n").append(manager.getMoodPrompt(
            blockEntry.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
    messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
}
```

Only the way `BudComponent` is fetched off the context differs. Fix: add a shared helper
to `AbstractLLMMessageCreation`, e.g.
`protected void appendMoodBlock(StringBuilder systemPromptBuilder, StringBuilder messageBuilder, BudComponent budComponent, LLMPromptManager manager)`,
and have all 13 classes call it instead of inlining the block.

### 3. `BudCreationQueue` reimplements `AbstractQueue` instead of extending it

`src/main/java/com/bud/feature/queue/creation/BudCreationQueue.java` extends
`AbstractTracker` and hand-rolls its own `ConcurrentLinkedQueue`, `addToCache()`, and
`startPolling()` (with a hardcoded 500ms interval), duplicating exactly what
`src/main/java/com/bud/feature/queue/AbstractQueue.java` already provides (250ms interval,
shared `cache`, shared `addToCache()`/`startPolling()`). Every sibling queue —
`StateChangeQueue`, `TeleportQueue` — extends `AbstractQueue` and only implements
`pollAndHandle()`. Fix: make `BudCreationQueue extends AbstractQueue`, drop its private
`cache`/`addToCache`/`startPolling` overrides, and implement just `pollAndHandle()` like
the others (folding `BudCreationEntry` into the shared `IQueueEntry`-typed `cache` if it
isn't already).

### 4. Duplicated hardcoded error string — done

Both call sites now go through a new shared `MemoryCommand.unknownBudMessage(rawBudName)`,
which derives the "Valid: ..." list from `BudRegistry.getInstance().getIds()`.

## Dead Code

### 1. `AbstractCache.getHistory()` / `pollHistory()` are never called — done

Verified zero callers anywhere in `src/main/java`, then deleted both methods.

## Wrong Separation (Configs)

### 1. `WorkConfig` mixes plain config storage with derived business logic

Every other config class (`LLMConfig`, `ReactionConfig`, `OrchestratorConfig`,
`ConversationConfig`, `DebugConfig`) is pure data: fields, a codec, and getters that
return the stored value as-is. `src/main/java/com/bud/core/config/WorkConfig.java:58-99`
breaks that pattern with three methods that compute derived values via hardcoded lookup
tables keyed off an enum:

```java
public int getFieldRadius(@Nonnull WorkRole workRole) {
    FieldSize size = getFieldSize(workRole);
    if (workRole == WorkRole.FARMING) {
        return switch (size) { case SMALL -> 4; case MEDIUM -> 5; case LARGE -> 6; };
    }
    return switch (size) { case SMALL -> 3; case MEDIUM -> 5; case LARGE -> 7; };
}
```

`getFieldSize()` also silently falls back to `MEDIUM` on a bad string and logs a warning —
real behavior, not configuration. This makes `WorkConfig` do two jobs (config holder +
field-sizing policy), which is exactly the kind of logic the rest of the codebase keeps out
of config classes. Fix: move `getFieldSize`/`getFieldRadius`/`getFieldStructureCount` into a
new `com.bud.feature.work.FieldSizing` (or similar) utility that takes a `WorkConfig` (or
the raw size strings) as input; leave `WorkConfig` itself as three plain
`getFarmingFieldSize()`/`getLumberingFieldSize()`/`getMiningFieldSize()` string getters.

### 2. Bud identity is hardcoded into commands instead of read from `BudRegistry`

CLAUDE.md's own description of the architecture: *"Buds are fully data-driven — there is
no `BudType` enum... other defined Buds remain summonable individually."* Several command
classes don't honor that:

- `src/main/java/com/bud/app/commands/MemoryCommand.java:27-38,65-90` declares three fixed
  `FlagArg`s (`veriFlag`, `keylethFlag`, `gronkhFlag`) and falls back to
  `Set.of("veri", "keyleth", "gronkh")` (line 75) when none is passed — a 4th
  server-defined Bud (via `buds/<id>.yml`) can never be filtered by `/bud memory`, and
  won't be included in the "all buds" default either.
- `src/main/java/com/bud/app/commands/ResetCommand.java:35-36` hardcodes
  `Set.of("veri", "keyleth", "gronkh")` for both cleanup and recreation on `/bud reset` —
  this is a functional bug, not just style: a player whose roster includes a custom 4th
  Bud will have it silently skipped by reset, and a player who never had `gronkh` will have
  it spawned anyway. Should instead reset whatever the player's `PlayerBudComponent`
  actually has (or `BudRegistry.getInstance().getDefaultBudIds()` if the intent is "reset to
  server defaults").
- `MemorySetCommand`/`MemoryDeleteCommand` (see "Redundant Code" #4) hardcode the same
  three names in their error string.

Fix: replace all of the above with lookups against `BudRegistry.getInstance().getIds()` /
`getDefaultBudIds()`, and generate `MemoryCommand`'s per-Bud flags dynamically (or drop
them in favor of a single `--bud <id>` argument validated against the registry) so new
Buds added purely via YAML stay fully supported without a code change.
