---
name: hytale-engine-lookup
description: 'Use this skill when you need to look something up in the Hytale engine itself — verify an SDK method signature/nullability, read a class''s bytecode, or find/inspect a native asset (block type, hitbox, prefab, farming stage) under reference/server or reference/assets — instead of guessing engine behaviour or reading it off decompiled-looking names.'
---

# Hytale Engine Lookup (bytecode + asset mirror)

`reference/` is a gitignored, hand-extracted mirror of the local Hytale install — see `CLAUDE.md`, "Reference: local Hytale install mirror". Two halves, two different tools, and mixing them up either fails loudly or (worse) silently returns nothing:

- **`reference/server/`** — ~37,000 raw `.class` files, no `.java` sources. Good for symbol names via bytecode, useless for reading decompiled logic.
- **`reference/assets/`** — ~33,000 native JSON/model assets (`Common/`, `Server/`, texture/model files).

## The trap this skill exists to prevent

The Grep tool (ripgrep) **silently skips binary files** — pointing it at `reference/server/**/*.class` returns "no matches" even when the symbol is there, with nothing telling you it didn't look. Conversely, plain `grep -r` over `reference/assets` (33k JSON files) routinely blows past a 2-minute shell timeout. Neither failure mode announces itself as a tooling problem — both look exactly like "the thing isn't there." A whole session was lost this way before the actual cause (grepping binaries with a binary-blind tool, and grepping a 33k-file JSON tree with a linear shell `grep`) was found.

**Rule of thumb:**
- Searching **inside `.class` files** (a symbol, a method name, a string constant) → `scripts/hytale-class.sh`, which shells out to plain `grep`/`javap` and knows where the JDK lives. Never the Grep tool here.
- Searching **inside `reference/assets`** (a JSON key, an item id, a block type reference) → the Grep tool (ripgrep), which is fast enough for 33k text files — never a raw shell `grep -r` loop there, it will time out.
- Reading one specific asset file once you have its path → the Read tool, either half.

## Bytecode lookups (`reference/server`)

```
.claude/skills/hytale-engine-lookup/scripts/hytale-class.sh find <symbol>
.claude/skills/hytale-engine-lookup/scripts/hytale-class.sh sig  <Class>
.claude/skills/hytale-engine-lookup/scripts/hytale-class.sh code <Class> [member]
```

- `find` — which class files mention a symbol (method name, field name, string constant). Start here when you don't know which class owns something.
- `sig` — `javap -p`, every member's signature. Use this to check a method's actual `@Nonnull`/`@Nullable` contract before overriding it (see `CLAUDE.md`, "Null-safety") — never guess the parent signature.
- `code` — `javap -p -c`, full bytecode. Pass a member name as the second argument to isolate just that method instead of scrolling a multi-hundred-line dump; without it, dumps over ~200 lines print only the member list so you can re-run targeted.

`<Class>` accepts a bare name (`BlockType`), an inner class (`BlockModule$BlockStateInfo`), a dotted fully-qualified name, or a path relative to `reference/server`. A bare name matching more than one class (e.g. `BlockType` exists in both `com.hypixel.hytale.protocol` and `com.hypixel.hytale.server.core.asset.type.blocktype.config`) errors out and lists the candidates — rerun with one of those paths.

Needs `JAVA_HOME` pointed at a JDK with `javap`; falls back to this repo's bundled `jdk-25.0.2/` automatically (see `CLAUDE.md`, "Build & run").

## Asset lookups (`reference/assets`)

No wrapper script needed — the Grep tool handles this tree fine as long as you scope it:

- Pass `path: "reference/assets"` (or narrower, e.g. `reference/assets/Server/Item/Items`) and a `glob` (`*.json`) so it isn't scanning texture/model binaries too.
- Prefer `output_mode: "files_with_matches"` first to find candidates, then Read the specific file — reference assets are often large (a tree prefab or a `Component_*_Instruction_*.json` can run thousands of lines) and dumping full content into a search is wasteful.
- For "does this native mechanic exist / what does Vanilla do here" questions, search for the mechanic by convention name (a tag like `Type=Soil`, a component key like `FarmingBlock`, a hitbox id) rather than guessing a file path — asset organization doesn't always match the block/item id.

## Verify, don't assume

Both halves exist because guessing engine behaviour from names has produced real bugs in this project (see `.claude/features/` for examples: a hitbox silently shrinking to 1x1x1 because a second, undocumented hitbox field existed; a block-existence check that looked right at compile time but resolved to `null` at runtime because the check ran before the engine had loaded block types). When behaviour is genuinely engine-internal rather than "how does this API work," bytecode analysis of `reference/server` is blind to it — client-side rendering/UI behaviour in particular can only be confirmed by an actual in-game test, not by any amount of `javap`. Say so rather than presenting a bytecode-plausible guess as verified.
