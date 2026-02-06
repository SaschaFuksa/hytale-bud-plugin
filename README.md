<div align="center">
  <h1>Hytale Bud Plugin</h1>
  <img src="https://fuksa.de/hytale/buddies.png" width="600px">
  <p><i>Used template by <a href="https://github.com/UpcraftLP">Up</a>, slightly modified by <a href="https://github.com/Kaupenjoe">Kaupenjoe</a>.</i><br>
  <i>Inspired by <a href="https://www.curseforge.com/hytale/mods/my-friends">MyFriends</a> by LukeysMods.</i></p>
</div>

<br>

## 🌟 Overview

This is a proof of concept (PoC) plugin for Hytale that integrates **Large Language Models (LLM)** to create truly interactive RPG-style companions. These "Buds" don't just follow you—they talk, react to the world, and remember your battles.

### Meet your Buddies

<table>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Veri.png" alt="Veri"></td>
    <td>
      <h3>🦊 Veri</h3>
      <p>A Feran buddy who is a little childish, but extremely curious and friendly. Veri is agile and supports you in combat using <b>daggers</b>.</p>
    </td>
  </tr>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Gronkh.png" alt="Gronkh"></td>
    <td>
      <h3>👹 Gronkh</h3>
      <p>A Trork buddy who might seem grumpy at first, but is fiercely loyal and strong. He's a powerhouse who supports you with a heavy <b>mace</b>.</p>
    </td>
  </tr>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Keyleth.png" alt="Keyleth"></td>
    <td>
      <h3>🍃 Keyleth</h3>
      <p>A Kweebec buddy who is highly intelligent but a bit shy. She prefers to keep her distance and supports you from afar with a <b>bow</b>.</p>
    </td>
  </tr>
</table>

<br>

## 🚀 Features

### 🎮 Commands
The plugin is primarily controlled via simple chat commands:

*   **`/bud`** - The main command. Spawns all three buddies, teleports them to you, or respawns them if they died.
*   **`/bud [Veri|Gronkh|Keyleth]`** - Target a specific buddy for spawning or teleportation.
*   **`/bud [attack|atk]`** - Change the behavior mode for all active Buds to Defensive.
*   **`/bud [follow|fol]`** - Change the behavior mode for all active Buds to Passive.
*   **`/bud [chill|stay]`** - Change the behavior mode for all active Buds to Sitting.
*   **`/bud reset`** - Quickly cleanup and recreate all your buddies.
*   **`/bud clean`** - Removes your personal buddies from the world.
*   **`/bud clean-all`** - Removes all buddies from the world.
*   **`/bud data`** - Displays persisted data (Reference Player UUID to NPC UUID).
*   **`/bud data-clean`** - Clears all persisted buddy data (useful for debugging).
*   **`/bud prompt-reload`** - Hot-reload the LLM prompt configurations without restarting the server.

### 🤖 Intelligent Interaction
*   **Dynamic Modes**: Toggle between *Defensive* (attacks), *Passive* (follows), and *Sitting* (stays put).
*   **World Awareness**: Buds send chat messages about current world information every few minutes.
*   **Combat Memory**: Your companions react to your recent fights with context-aware dialogue.

<br>

## ⚙️ Configuration (LLM)

To enable the AI features, edit the `HytaleBud.json` in your server's mod folder:

| Setting | Description | Default |
|:--- |:--- |:--- |
| `EnableLLM` | Toggle LLM features | `true` |`
| `UsePlayer2API` | Toggle to use Player2 API for LLM (EnableLLM must be true) | `false` |
| `Url` | Your LLM API Endpoint | `v1/chat/completions` |
| `Model` | The AI model identifier | `ibm/granite-4-h-tiny` |
| `ApiKey` | The API key for your LLM service | `not_needed` |
| `MaxTokens` | Limit the length of AI responses | `200` |
| `Temperature` | Control randomness (0.0 - 1.0) | `0.8` |

<br>

## 🛠️ Development

### Dev Workflow
1.  **Initial Setup**: `.\gradlew decompileServer`
2.  **Build**: `.\gradlew build`
3.  **Run Server**: `.\gradlew runServer`

<br>

## 🗺️ Roadmap

- [x] **1.3.0** (Current): Separated prompts from code (YAML), reduced token usage (max 200), implemented staggered spawning (0.5s interval). Add chat reactions for non LLM usage (Only generic one for each interaction).
- [ ] **1.4.0**: Memory storage for environment events (e.g., "You just mined 20 blocks of iron ore!").
- [ ] **1.5.0**: Add "days" in environment event and try to get events like rainy, snow, sandstorm... 
- [ ] **1.6.0**: Try a "horde-wave"-event each wednesday and saturday evening (Horde mobs spawn in near of player, is attracted to player)
- [ ] **1.7.0**: ...
- [ ] **1.8.0**: Visual updates & special models for Buds?
- [ ] **1.9.0**: Item-based spawning instead of commands?
- [ ] **2.0.0**: Fully interactive world manipulation via LLM?
<br>

## 📜 History

For a detailed list of all changes and version history, please see the [CHANGELOG.md](CHANGELOG.md).
---
<div align="center">
  <p><i>"This mod will also work without LLM, but the main goal is to have interactive buddies."</i></p>
  <sub>Created with ❤️ for the Hytale Community</sub>
</div>
