---
name: regression-checklist-sync
description: 'Use this skill whenever testing/regression-checklist.html is edited, or when asked to "update the regression checklist" / "publish the checklist" / "update the website" for it — keeps the repo file and its published Artifact copy from silently diverging.'
---

# Regression Checklist Sync

`testing/regression-checklist.html` (repo, versioned, source of truth) and its published Artifact
copy are **two separate files that do not sync themselves**. Editing one and forgetting the other
is exactly what happened once already this project — the repo file got three rounds of edits
before anyone noticed the live Artifact link was still showing the old version. Every edit to the
repo file must end with a republish, in the same turn, not "later."

**Permanent Artifact URL** (always publish to this one, never a fresh one, so the link the user has
open in their browser stays valid): `https://claude.ai/code/artifact/59335380-b805-470b-915c-6d647a85f387`

Title `Bud Plugin Regression Pass`, favicon `✅` — keep both stable across republishes; the
artifact-design skill's rule against changing a stable favicon applies here too.

## Why a plain republish of the repo file doesn't work

`testing/regression-checklist.html` is a complete, standalone document (`<!doctype>`, `<html>`,
`<head>`, `<body>` — it has to open correctly via a plain double-click, no server). The Artifact
tool expects the opposite: a fragment starting with a bare `<title>`, no wrapper tags, because it
adds its own wrapper at publish time. Publishing the repo file's raw content as-is would nest a
second `<html>/<head>/<body>` inside the Artifact's own — broken rendering, not just a lint issue.

## Workflow

1. Edit `testing/regression-checklist.html` directly (the repo copy) — this is the only file you
   actually author content in.
2. Derive the Artifact fragment from it:
   ```
   python .claude/skills/regression-checklist-sync/scripts/extract_artifact_fragment.py \
     testing/regression-checklist.html <scratchpad_dir>/regression-checklist.html
   ```
   Use this turn's scratchpad directory (see the system prompt) as the output location — it's
   session-local and gets recreated fresh each session, which is exactly why the permanent URL
   above (not a remembered file path) is the thing that ties republishes together across sessions.
3. Publish that fragment with the Artifact tool: `file_path` = the scratchpad output from step 2,
   `url` = the permanent URL above, `favicon` = `✅`, `title`/`description` unchanged from prior
   publishes (see `docs/index.html`'s own copy for the current description wording if unsure).
4. Sanity-check before calling it done — both files must have the same checkbox set:
   ```
   grep -oE 'id="i-[a-z0-9]+"' testing/regression-checklist.html | sort > /tmp/repo_ids.txt
   grep -oE 'id="i-[a-z0-9]+"' <scratchpad_dir>/regression-checklist.html | sort > /tmp/frag_ids.txt
   diff /tmp/repo_ids.txt /tmp/frag_ids.txt   # must be empty
   grep -oE 'id="i-[a-z0-9]+"' testing/regression-checklist.html | sort | uniq -d   # must be empty (no dup ids)
   ```

## The "This session's changes" section (`#sec-regressions`)

This section is deliberately **ephemeral**, not a permanent history — its own subtitle says "check
these first." It exists to smoke-test whatever is riskiest right after a work session, then gets
cleared once the user has verified those items, ready for the next round. Concretely:

- After finishing a chunk of fixes/features in a session, add one short item per risky change here
  (not a full description — one line + an optional `.item-detail` for how to trigger it).
- Once the user confirms they've verified the section (e.g. "das passt, ist validiert"), **empty
  the section body** back to just the structural shell — don't leave stale entries accumulating,
  and don't move them into the permanent sections either (a change that matters long-term already
  has, or should get, its own line in the relevant permanent section like Lumbering/Commands/etc. —
  this section is not that; it's a checklist that resets, not an appendix).
- Never add an item here for something not yet confirmed fixed — this section means "verify this
  is actually right," not "here's what we attempted." An unresolved bug stays out until fixed.

Both the repo file and the Artifact fragment need this clear/add to happen together, same as any
other edit — step 1–4 above apply here too.
