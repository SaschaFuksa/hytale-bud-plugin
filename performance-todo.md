# Performance TODO

Findings from a pass over `src/main/java` looking for CPU/allocation hotspots. Ordered
roughly by expected impact. File:line references point at the code as of this pass —
re-check before fixing since neighboring lines shift.

## 1. Work field scans re-walk the whole field multiple times per assignment (highest impact) — partially done

`WorkstationFuelTickSystem.findNextWorkAssignment()` runs whenever a bound Bud has no current
work target (i.e. after every completed action, and every `IdleRetrySeconds` — default 5s —
while idle). Three of the four sub-issues found here are fixed:

- **Done** — the position list (`FieldCandidates.serpentinePositions` /
  `LumberingFieldScan.treeEdgePositions`) is no longer rebuilt from scratch every call; it's
  cached per-Workstation on `WorkstationBlockEntity` (`cachedSerpentinePositions`/
  `cachedEdgePositions`), keyed by anchor+radius+height(+edgeCount), and reused until one of
  those actually changes. Lumbering's second list (`fellPositions`) uses the same cache slot.
- **Done** — `isNeverWateredCandidate`/`isFertilizeCandidate`/`isWaterRefreshCandidate` used to
  each independently re-fetch the same `TilledSoilBlock` component for the same position
  across three separate passes. Merged into one loop backed by
  `FieldCandidates.resolveTilledSoilCandidates()`, which fetches it once per position.
- **Done, farming/lumbering only** — `FieldCandidates.isTooCloseToExistingTree()` no longer
  does a fresh `O(minDistance²)` block-lookup scan per plant candidate; a
  `Set<Vector3i>` of existing tree-trunk positions is built once per scan
  (`FieldCandidates.collectExistingTreePositions()`, scanning `fieldRadius + treeMinDistance`
  around the anchor at the fixed trunk Y-layer) and the per-candidate check is now a set
  lookup. **Not applied to `MiningFieldScan.isTooCloseToGrowthBlock()`**: its neighborhood
  check spans a Y-band relative to each candidate's own height rather than one fixed layer, so
  a correct global precompute would cost roughly as much to build as doing it inline — not a
  clean win, left as-is.
- **Not done** — merging the up to 7 separate linear passes (prepare-soil, till, plant,
  water/fertilize, harvest) into one combined per-cell pass. The passes themselves are cheap
  iteration over an already-materialized list; the above three fixes already remove the actual
  duplicate/expensive engine lookups. A full merge would be a much larger rewrite of the core
  winner-selection logic for comparatively little additional win on top of the above — only
  worth revisiting if the above turns out insufficient in practice.

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
