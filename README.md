<div align="center">
  <h1>Hytale Bud Plugin</h1>
  <img src="https://fuksa.de/hytale/buddies.png" width="600px">
  <p><i>Used template by <a href="https://github.com/UpcraftLP">Up</a>, slightly modified by <a href="https://github.com/Kaupenjoe">Kaupenjoe</a>.</i><br>
  <i>Inspired by <a href="https://www.curseforge.com/hytale/mods/my-friends">MyFriends</a> by LukeysMods.</i></p>
</div>

<br>

## 🌟 Overview

This is a proof of concept (PoC) plugin for Hytale that integrates **Large Language Models (LLM)** to create truly interactive RPG-style companions. These "Buds" don't just follow you—they talk, react to the world, and remember your battles.

## Update information:

If you have an older version of the plugin, remove the **/prompts** folder or update it after you added an newer version with **/bud prompt-reload**.

## New in 1.4.0
- **Block Interaction**: Buds will now respond to block placements and breaks by the player, providing feedback on the block type and action.
- **Event Registration Control**: Added enable/disable options for combat, world, and block interactions in the config.
(See more changes in GitHub Repository in the **CHANGELOG.md**

### Meet your Buddies

<table>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Veri.png" alt="Veri"></td>
    <td>
      <h3>🦊 Veri</h3>
      <p>A Feran buddy who is a little childish, but extremely curious and friendly. Veri is agile and supports you in combat using <b>daggers</b>.</p>
      <p>Veri travels with you to find the rare antidote for his sick Feran clan infected by strange disease from the toxic Skaraks.</p>
    </td>
  </tr>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Gronkh.png" alt="Gronkh"></td>
    <td>
      <h3>👹 Gronkh</h3>
      <p>A Trork buddy who might seem grumpy at first, but is fiercely loyal and strong. He's a powerhouse who supports you with a heavy <b>mace</b>.</p>
      <p>Gronkh is on a mission to protect the northern tribe from the icy 'white wanderers' and is training to crush the undead forces threatening their lands.</p>
    </td>
  </tr>
  <tr>
    <td width="200", padding="10px"><img src="https://fuksa.de/hytale/Keyleth.png" alt="Keyleth"></td>
    <td>
      <h3>🍃 Keyleth</h3>
      <p>A Kweebec buddy who is highly intelligent but a bit shy. She prefers to keep her distance and supports you from afar with a <b>bow</b>.</p>
      <p>Keyleth is on a quest to uncover ancient knowledge in lost temples. She wants to balance by connecting old temples to defeat Shadow Knight and the dark army.</p>
    </td>
  </tr>
</table>

<br>

## 🚀 Features

### 🎮 Commands
The plugin is primarily controlled via simple chat commands:

*   **`/bud`** - The main command. Spawns all three buddies, teleports them to you, or respawns them if they died.
*   **`/bud [Veri|Gronkh|Keyleth]`** - Target a specific buddy for spawning or teleportation.
*   **`/bud [attack|atk]`** - Change the behavior mode for all active Buds to Defensive.s
*   **`/bud [follow|fol]`** - Change the behavior mode for all active Buds to Passive.
*   **`/bud [chill|stay]`** - Change the behavior mode for all active Buds to Sitting.
*   **`/bud reset`** - Quickly cleanup and recreate all your buddies.
*   **`/bud clear`** - Removes your personal buddies from the world.
*   **`/bud clear-allbuds`** - Removes all buddies from the world.
*   **`/bud clear-data`** - Clears all persisted buddy data (useful for debugging).
*   **`/bud clear-prompt`** - Hot-reload the LLM prompt configurations without restarting the server.
*   **`/bud data`** - Displays persisted data (Reference Player UUID to NPC UUID).

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
| `UsePlayer2API` | Toggle to use Player2 API for LLM <br>(EnableLLM must be true) | `false` |
| `Url` | Your LLM API Endpoint | `v1/chat/completions` |
| `Model` | The AI model identifier | `ibm/granite-4-h-tiny` |
| `ApiKey` | The API key for your LLM service | `not_needed` |
| `MaxTokens` | Limit the length of AI responses | `200` |
| `Temperature` | Control randomness (0.0 - 1.0) | `0.8` |
| `EnableCombatReactions` | Enable or disable combat reaction messages | `true` |
| `EnableWorldReactions` | Enable or disable world reaction messages | `true` |
| `EnableBlockReactions` | Enable or disable block reaction messages | `true` |

### 🧠 Prompt Management
The LLM prompts are now stored in external `YAML` files located in the mod folder. This allows for easier editing and customization of NPC personalities without modifying the code. Each buddy has its own prompt file, and there are prompts for world interactions.

First time the server starts, the default prompts will be copied from the resources to the mod folder. You can then edit these files to customize the behavior and personality of your Buds.

**Attention**: The command `/bud prompt-reload` will overwrite the existing prompt files with the default ones from the resources. Make sure to backup your custom prompts before using this command.

### ⚠️ LLM Performance Note (Reasoning Models)

If you are using **Reasoning Models** (e.g., DeepSeek-R1, Qwen-Reasoning):
*   **Disable "Thinking":** These models generate many `<think>` tokens which can cause delays or cut-off messages. It is recommended to use models without a "thinking" phase or to disable it in your API provider's settings.
*   **Token Limit:** If messages are cut off, increase `MaxTokens` in your config to at least `500`.
*   **Filter:** The plugin automatically tries to filter `<think>` tags, but native "No-Thinking" models provide the best experience.

<br>

## 🛠️ Development

### Dev Workflow
1.  **Initial Setup**: `.\gradlew decompileServer`
2.  **Build**: `.\gradlew build`
3.  **Run Server**: `.\gradlew runServer`

<br>

## 🗺️ Roadmap

- [x] **1.5.0**: Interaction to weather events and add "days"
- [ ] **1.6.0**: Memory storage: Keep memories of player and buds interacttions. Also remove current caching of the last combat action and use the memory storage instead.
- [ ] **1.7.0**: ?
- [ ] **1.8.0**: Visual updates & special models for Buds?
- [ ] **1.9.0**: Item-based spawning instead of commands?
- [ ] **2.0.0**: Interactive world manipulation via LLM? Or try a "horde-wave"-event each wednesday and saturday evening (Horde mobs spawn in near of player, is attracted to player)?
<br>

## 📜 History

For a detailed list of all changes and version history, please see the [CHANGELOG.md](CHANGELOG.md).
---
<div align="center">
  <p><i>"This mod will also work without LLM, but the main goal is to have interactive buddies."</i></p>
  <sub>Created with ❤️ for the Hytale Community</sub>
</div>
