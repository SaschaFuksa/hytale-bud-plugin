<div align="center">
  <h1>Hytale Bud Plugin</h1>
  <img src="docs/images/buddies.avif" width="600px">
  <p><i>Used template by <a href="https://github.com/UpcraftLP">Up</a>, slightly modified by <a href="https://github.com/Kaupenjoe">Kaupenjoe</a>.</i><br>
  <i>Inspired by <a href="https://www.curseforge.com/hytale/mods/my-friends">MyFriends</a> by LukeysMods.</i></p>
  Bud Work Feature inspired by <a href="https://www.curseforge.com/hytale/mods/ancient-constructs">Ancient Constructs</a> by <a href="https://www.curseforge.com/members/danbagh/projects">DanBagh</a>.
</div>

<br>

<div align="center">

📖 **[Full documentation, guides and configuration reference →](https://saschafuksa.github.io/hytale-bud-plugin/)**

</div>

<br>

## 🌟 Overview

This is a proof of concept (PoC) plugin for Hytale that integrates **Large Language Models (LLM)** to create truly interactive RPG-style companions. These "Buds" don't just follow you—they talk, react to the world, and remember your battles. The mod works fully without an LLM configured too — the AI layer is what makes it come alive, not what it depends on.

<b>Only tested in single player mode!</b>

> Upgrading from an older version? Remove the `/prompts` folder, or update it with `/bud prompt reset`.

### New in 2.0.0.:

- **Bud 2 Bud Reactions**: Buds now react to each other's mood changes, spawns, state changes (e.g. sitting down), and legendary memories, and can chain into a couple of named back-and-forth replies before going quiet again
- **Bud 2 Bud Memories**: Your buds will now have persisted memories about other buds, including a shared "legendary" memory between a pair of buds
- **Bud Player State Reactions**: Your buds will now react if you have status effects like poisoned, health regeneration e.g.
- **Data-driven Bud registry**: Buds are no longer a hardcoded `BudType` enum. Each companion is now defined by a `buds/<id>.yml` file (`BudRegistry`/`BudDefinition`), with `buds/roster.yml` controlling the default roster `/bud create` spawns without an argument. New Buds only need a new YAML file plus matching game assets — no plugin rebuild.
- **New Commands**:
  - `/bud memory set <bud> "<text>" [--legendary]` — manually add a memory (or legendary memory) for a Bud.
  - `/bud memory delete <bud> <index> [--legendary]` — remove a memory by index. `/bud memory` and `/bud memory --legendary` now print the index of each entry so you know what to pass here.
  - `/bud delete --playername <name>` — delete another player's Buds (admin only).
  - `/bud create <bud>` now takes a generic, tab-completed Bud id instead of fixed `--veri`/`--gronkh`/`--keyleth` flags.
- **Central Version Management**: Global version file and prompt/bud-config reloading
- **Bud Roster Spawning**: New card item to spawn/despawn all roster Buds at once, without needing to use commands. The card can be crafted on the arcane workbench.
- **Bud Cards**: Optical rework and texture now matches the card.
- **Bud Work Stations**: Now you can send your bud to work an harvest/lumber/mine items

### Meet the Buddies

<table>
  <tr>
    <td width="110"><img src="docs/images/veri.avif" alt="Veri"></td>
    <td><b>🦊 Veri</b> — Feran, agile and curious, fights with daggers. <i>Worker role: Mining.</i></td>
  </tr>
  <tr>
    <td width="110"><img src="docs/images/gronkh.avif" alt="Gronkh"></td>
    <td><b>👹 Gronkh</b> — Trork, grumpy but loyal, fights with a mace. <i>Worker role: Lumbering.</i></td>
  </tr>
  <tr>
    <td width="110"><img src="docs/images/keyleth.avif" alt="Keyleth"></td>
    <td><b>🍃 Keyleth</b> — Kweebec, shy and intelligent, supports with a bow. <i>Worker role: Farming.</i></td>
  </tr>
</table>

Every Bud is fully data-driven — new companions are added via a YAML definition, no plugin rebuild required. → [Meet the Buddies in full](https://saschafuksa.github.io/hytale-bud-plugin/#buddies)

<br>

## 🚀 Features

- **LLM Reactions** — your Buds react in character to combat, block placement, crafting, item pickups, weather, new zones, your chat, even your status effects — generated live by an LLM, or a hand-written fallback line when none is configured.
- **Bud Cards + Crafting** — summon and dismiss companions with a craftable card instead of chat commands. → [How to craft & use them](https://saschafuksa.github.io/hytale-bud-plugin/cards.html)
- **Bud Work Stations** — assign a Bud to a Workstation and it farms, logs, or mines a field on its own. → [How Work Stations work](https://saschafuksa.github.io/hytale-bud-plugin/work-stations.html)

→ [See every feature](https://saschafuksa.github.io/hytale-bud-plugin/#features)

<br>

## 🆕 New Features

Highlights of the latest release:

- **Bud-to-Bud Reactions & Memories** — Buds react to each other and build shared memories.
- **Bud Work Stations** — send a Bud to farm, log, or mine autonomously.
- **Data-driven Bud Registry** — new companions via YAML, no rebuild.
- **Central content versioning** — prompts and Bud definitions auto-update safely.

→ [Full changelog](https://github.com/SaschaFuksa/hytale-bud-plugin/blob/main/CHANGELOG.md)

<br>

## 🎮 Commands

Everything runs through `/bud <subcommand>` — creation, deletion, state, memory, prompts, reload, debug.

→ [Full command reference](https://saschafuksa.github.io/hytale-bud-plugin/commands.html)

<br>

## ⚙️ Configuration

Six JSON files, one per concern (LLM, Reaction, Orchestrator, Work, Debug, Conversation), plus the Bud Registry and prompt YAMLs.

→ [Full configuration reference](https://saschafuksa.github.io/hytale-bud-plugin/configuration.html)

<br>

## 🛠️ Development

1.  **Initial Setup**: `.\gradlew decompileServer`
2.  **Build**: `.\gradlew build`
3.  **Run Server**: `.\gradlew runServer`
4.  **Auth Login**: `/auth login device`
5.  **Persist Login**: `/auth persistence Encrypted`

<br>

## Known Issues

- After player teleport, buds are often broken and are "invisible". Workaround: Use "/bud" or "/bud reset" command to respawn them.

<br>

## 📜 History

For a detailed list of all changes and version history, please see the <a href="https://github.com/SaschaFuksa/hytale-bud-plugin/blob/main/CHANGELOG.md">CHANGELOG.md</a>.

---
<div align="center">
  <p><i>"This mod will also work without LLM, but the main goal is to have interactive buddies."</i></p>
  <sub>Created with ❤️ for the Hytale Community</sub>
</div>
