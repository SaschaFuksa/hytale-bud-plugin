# Improvement TODO

Findings from a pass over `src/main/java` looking for bad code quality, redundant code,
dead code, and configs that hold the wrong kind of thing. Grouped by category, ordered
roughly by impact within each group. File:line references point at the code as of this
pass — re-check before fixing since neighboring lines shift.

## Bad Code Quality

### 1. Default LLM config ships what looks like a real API key and a personal LAN IP

`src/main/java/com/bud/core/config/LLMConfig.java:13-15`:

```java
private String url = "http://192.168.178.25:1234/v1/chat/completions";
private String model = "mistralai/ministral-3-3b";
private String apiKey = "sk-lm-KbCP0975:4MGo9MUOSThOoMCmP9CG";
```

These are compiled into the jar as the field defaults used the moment `LLMConfig.CODEC`
first writes out `LLM.json` on a fresh server (`BudPlugin.setupConfig()` calls
`this.llmConfig.save()` right after construction). Whether or not this specific key is
still live, shipping a credential-shaped default in source control trains
users/contributors to treat it as normal, and it's a personal LAN address that has no
business being a public default. Fix: default `url`/`apiKey` to `""` (or a placeholder
like `"http://localhost:1234/v1/chat/completions"` / `"sk-..."`), and document in the
README that they must be filled in — which is exactly the pattern `LLMConfig.isEnableLLM()`
already exists to gate around.

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

### 4. Singleton thread-safety is inconsistent across near-identical classes

Every `com.bud.core.config.*` class uses `private static volatile T instance;` for its
singleton field (`LLMConfig`, `ReactionConfig`, `OrchestratorConfig`, `ConversationConfig`,
`DebugConfig`, `WorkConfig`), but three other singletons that follow the exact same
"lazy-init in `getInstance()`" pattern omit `volatile`:

- `src/main/java/com/bud/core/registry/BudRegistry.java:31`
- `src/main/java/com/bud/feature/LLMPromptManager.java:30`
- `src/main/java/com/bud/feature/work/WorkRecipeConfig.java:27`

All three are read from LLM-call virtual threads as well as the world thread (e.g.
`BudRegistry.getInstance().get(budId)` from `LLM*MessageCreation` classes). Without
`volatile`, a thread that didn't perform the lazy-init has no guarantee of seeing the
assignment promptly. In practice this window is startup-only and narrow, but the
inconsistency itself is the bug magnet — a future edit that adds a genuine `getInstance()`
race (e.g. behind a reload command) will silently inherit the missing `volatile`. Fix: add
`volatile` to all three for consistency with the established config-class pattern.

### 5. Inconsistent indentation in `LLMCombatMessageCreation`

`src/main/java/com/bud/feature/combat/LLMCombatMessageCreation.java:14-84` indents the
entire class body with 8 spaces instead of the 4-space convention used by every sibling
`LLM*MessageCreation` class (compare `LLMBlockMessageCreation.java` or
`LLMCraftMessageCreation.java`). Cosmetic, but worth a reformat pass since it's the one
outlier file.

### 6. "Constants" declared as non-static final instance fields

`src/main/java/com/bud/feature/queue/AbstractQueue.java:14-15`:

```java
private final long INITIAL_DELAY_MS = 250L;
private final long POLLING_INTERVAL_MS = 250L;
```

`UPPER_SNAKE_CASE` signals a constant, but these are per-instance fields, not `static`.
Harmless here only because every subclass is itself a singleton, but it's misleading and
inconsistent with `AbstractCache.MAX_HISTORY` (`protected static final int MAX_HISTORY = 3`)
in the same package. Fix: make both `private static final`.

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

### 4. Duplicated hardcoded error string

`src/main/java/com/bud/app/commands/MemorySetCommand.java:56` and
`src/main/java/com/bud/app/commands/MemoryDeleteCommand.java:59` both hardcode the
identical literal `"Unknown bud: " + rawBudName + ". Valid: veri, gronkh, keyleth."`. Beyond
the duplication, see "Wrong separation" #3 below — this list goes stale the moment a
server operator adds a fourth Bud. Fix along with that item: derive the message from
`BudRegistry.getInstance().getIds()` in one shared helper (e.g. on `MemoryCommand`, which
both subcommands already call into for `resolveBudDisplayName`).

## Dead Code

### 1. `AbstractCache.getHistory()` / `pollHistory()` are never called

`src/main/java/com/bud/feature/AbstractCache.java:20-33`:

```java
public LinkedList<IQueueEntry> getHistory(String playerName) { ... }
public IQueueEntry pollHistory(String playerName) { ... }
```

Neither method has a single caller anywhere in `src/main/java` (checked across the whole
tree). They're inherited by every `Recent*Cache` subclass as public API, so they're not
even self-contained — removing them shrinks the surface of five classes at once. Fix:
delete both, or if they were added for planned-but-unbuilt functionality (e.g. a debug/
inspection command), wire that up now or note it in a TODO instead of leaving live-looking
dead API on a shared base class.

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
