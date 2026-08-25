# Performance TODO

Findings from a pass over `src/main/java` looking for CPU/allocation hotspots. Ordered
roughly by expected impact. File:line references point at the code as of this pass —
re-check before fixing since neighboring lines shift.

## 1. Work field scans re-walk the whole field multiple times per assignment (highest impact)

`WorkstationFuelTickSystem.findNextWorkAssignment()` (`src/main/java/com/bud/feature/work/WorkstationFuelTickSystem.java:315`)
runs whenever a bound Bud has no current work target (i.e. after every completed action,
and every `IdleRetrySeconds` — default 5s — while idle). At `MEDIUM` field size that's
already ~5×5=25 radius-squared → ~400 `Vector3i` positions per scan (farming/mining), more
for `LARGE`. Per call it currently:

- Rebuilds the full position list from scratch every time (`FieldCandidates.serpentinePositions`
  / `LumberingFieldScan.treeEdgePositions`, both fresh `ArrayList<Vector3i>` with a new
  `Vector3i` per cell — lines 322-325).
- Does **up to 7 separate full linear passes** over that same list — one each for
  prepare-soil, till, plant, water-new, fertilize, harvest, water-refresh (lines 338-421) —
  instead of one pass that evaluates all candidate kinds together and short-circuits.
- For lumbering, recomputes an entirely separate full `serpentinePositions` list
  (`fellPositions`, line 468) on top of the edge-only list already computed above.
- The plant-candidate loop calls `FieldCandidates.isTooCloseToExistingTree()`
  (`src/main/java/com/bud/feature/work/FieldCandidates.java:72`), which does its own nested
  `O(minDistance²)` block-lookup loop **per candidate position** — so a field with no valid
  plant spot can multiply out to (positions × minDistance²) block lookups in the worst case.
  `MiningFieldScan.isTooCloseToGrowthBlock()` (`src/main/java/com/bud/feature/work/mining/MiningFieldScan.java:48`)
  has the identical shape for mining.

Each "block lookup" here is a chunk/component resolve (`world.getBlockType`,
`world.getBlockComponentHolder`), not a cheap array read, so this adds up fast with
multiple working Buds on a server. Fix direction: single combined pass over positions that
evaluates every candidate kind per cell and tracks the best winner per category, cache/reuse
the position list per anchor+radius (it only changes when config changes), and hoist the
proximity check out of the inner candidate loop (e.g. precompute a spatial set of existing
tree/growth positions once per scan instead of re-deriving it per candidate).

## 2. `JsonUtils` compiles a fresh regex `Pattern` on every field extraction

`src/main/java/com/bud/llm/client/JsonUtils.java:42-86` — `extractString`, `extractInt`,
`extractBoolean`, and `extractStringArray` all do
`Pattern.compile(TEMPLATE.pattern().formatted(Pattern.quote(key)))` inline, so every single
call recompiles a regex from scratch. These run on every LLM response parse (structured
memory summaries, legendary-replacement decisions, any future structured JSON use), often
several times per response (one per field). Fix: cache compiled patterns in a
`ConcurrentHashMap<String, Pattern>` keyed by `(templateName, key)`, or restructure to parse
the JSON once instead of re-scanning the raw string per field.

## 3. Regex-based text cleanup recompiles patterns on every LLM response

- `AbstractLLMClient.extractContent()` (`src/main/java/com/bud/llm/client/AbstractLLMClient.java:27-29`)
  calls `content.replaceAll(...)` twice with regex literals — each `String.replaceAll` call
  compiles a new `Pattern` internally. Runs on every LLM response.
- `LLMCaller.sanitizeResponse()` (`src/main/java/com/bud/llm/LLMCaller.java:84`) does the
  same with `sanitized.replaceAll("\\s+", " ")`.

Fix: hoist these to `private static final Pattern` constants (the class already does this
correctly for `TRAILING_FORMATTED_ASIDE`/`TRAILING_LABELED_META` in `LLMCaller` — just
missing for the two `replaceAll` call sites above).

## 4. Conversation memory stores scan every player/bud on every lookup

`RegularMemoryStore` and `LegendaryMemoryStore`
(`src/main/java/com/bud/feature/chat/conversation/RegularMemoryStore.java` and
`LegendaryMemoryStore.java`) key their backing maps as `ownerKey + "::" + budName` (or a
sorted pair key) but then implement per-owner reads by **iterating every entry in the map**
and checking `key.startsWith(ownerPrefix)`:

- `RegularMemoryStore.collectForOwner()` (line 109) — backs `getForOwner()` and
  `filterRelevant()`, called on every `augmentPrompt()` (i.e. every LLM interaction) and
  every `persistOwnerMemories()` (every memory-write).
- `LegendaryMemoryStore.collectForBud()` (line 38) and `snapshotForOwner()` (line 123) — same
  pattern, plus `collectForBud` does a `key.substring(...).split("\\|", 2)` (fresh regex
  compile per entry, see #2/#3 pattern) for every pair-keyed bucket on every call.
- `removeById`/`clearForOwner` in both stores are the same linear scan.

Cost scales with **total memory buckets across all players and Buds**, not with the one
player being served, so this gets linearly worse as the player count (or per-player Bud
count) grows on a busy server. Fix: key the outer map by normalized owner
(`Map<String, Map<String, List<...>>>`) so per-owner reads are a single `get()`.

## 5. `WorkRecipeConfig.isOreBlock` / `getOreTargetBlock` do a suffix scan per block

`src/main/java/com/bud/feature/work/WorkRecipeConfig.java:287-320` — both methods loop over
`ORE_BLOCK_SUFFIXES` (9 entries) doing `String.endsWith`/`BlockType.fromString` per call.
`isOreBlock` is called from `MiningFieldScan.isOreBlock`, which sits in the innermost loop of
`scanNodes()`'s per-column vertical search (§1) — so during a mining field scan this runs
many times per tick assignment. Fix: precompute a `Set<String>` (or prefix map) of
resolvable ore block IDs once when the recipe config loads, and do an O(1) lookup instead of
re-deriving it from suffixes every call.

## 6. Orchestrator tick scans all players × all channels every tick

`Orchestrator.tick()` (`src/main/java/com/bud/feature/queue/orchestrator/Orchestrator.java:130`)
iterates every tracked player every `OrchestratorTickIntervalMs` (default tick), and for each
player, `purgeStale()` and `pickChannel()` both iterate all 5 `OrchestratorChannel` values and
take a `synchronized` lock on each channel's queue even when it's empty. Fine at small player
counts; worth revisiting if the player count target grows, e.g. by skipping channels known to
be empty via a cheap non-synchronized `isEmpty()` check before entering the synchronized
block, or tracking a per-player "has any queued work" flag to skip idle players entirely.

## 7. Minor / low-risk cleanups

- `AbstractCache.getHistory()` (`src/main/java/com/bud/feature/AbstractCache.java:20`) does
  `cache.getOrDefault(playerName, new LinkedList<>())` — the fallback `LinkedList` is
  allocated on every call even on the (common) hit path where the key exists. Use
  `cache.get(playerName)` and null-check instead.
- `ItemPickupFilterSystem` (`src/main/java/com/bud/feature/item/ItemPickupFilterSystem.java:28-38`)
  assigns a `static Pattern RELEVANT_ITEMS_PATTERN` from the instance constructor. Not
  currently a hot-path issue (one instance is constructed at startup), but if the system is
  ever re-instantiated (hot reload, tests) the regex gets needlessly recompiled and the
  static mutation from an instance ctor is a footgun. Make it instance-level (or build it in
  a static initializer from a static source of truth).
- `BudManager.getAllBuds()`/`getAllPlayers()` (`src/main/java/com/bud/core/BudManager.java:352-419`)
  do a full parallel entity-store scan every call. Currently only called from periodic
  trackers (e.g. `MoodTracker`), which is fine, but any future hot-path caller should be
  steered toward `BudManager.getTrackedPlayers()` + component lookups instead of a full scan.
