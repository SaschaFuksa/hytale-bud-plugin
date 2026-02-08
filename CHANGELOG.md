# Changelog

All notable changes to this project will be documented in this file.

## [1.4.0]
### Added
- **Weather Interaction**: Buds will now respond to weather changes.
- **Week System**: Added week system, so the current day will also enable reactions.

### Fixed
- 

---

## [1.4.0]
### Added
- **Block Interaction**: Buds will now respond to block placements and breaks by the player, providing feedback on the block type and action.
- **Event Registration Control**: Added enable/disable options for combat, world, and block interactions in the config.

### Fixed
- Disable thinking for models that support it (e.g. Qwen) to prevent long response times and ensure concise answers.
- Try to avoid quotes in responses by instructing the LLM to not use them.
- Added fallback zone message for when no specific zone info is available, preventing null pointer exceptions and providing a default response.
- Improved code structure and organization for better readability and maintainability, including separating concerns and improving method naming.
- Now set system prompt correctly.
- Fixed bug in zone detection logic. Changed check of numeric value to "Zone1" to avoid issues with zone names containing numbers (e.g. "Zone2_1").

---

## [1.3.0]
### Added
- **YAML Prompt Management**: Refactored logic to use external `prompt.yml` files for easier editing of NPC personalities.
- **Default Chat Messages**: If LLM is disabled or fails, Buds will now respond with default messages for combat or world view interactions.
- **Token Logging**: Consumed tokens are now logged for every LLM interaction.

### Fixed
- Buds now spawn with a 0.3s delay between each other.
- Make it more random by adding a small random offset to the spawn location of each Bud.
- Spawning results are now handled in a thread-safe list to provide immediate feedback while work continues in the background.
- Corrected name of mod folder to "Bud_Plugin"
- Now handle better enabled LLM state with no activ LLM instance. Make sounds/chat more robust.
- Corrected bud names in roles definitions to match actual NPC names (Gronkh, Veri, Keyleth).

---

## [1.2.2]
### Fixed
- issue with shadow jar build where Jackson classes were not found at runtime.
- Fixed issue with permissions (restored missing override method). Now non-admin players can use `/bud` commands.

---

## [1.2.0]
### Added
- **Config Options**: Added `MaxTokens` and `Temperature` to the config.
- **Player2 API**: Implemented Player2 API support for easier AI integration.
- **Audio Feedback**: Bud sounds now play after a message is sent.

### Fixed
- Code improvements for sync/async method handling.
- Combat chat switched from polling (10s) to event-driven (2s delay after combat).
- Added 3s cooldown for combat messages.

---

## [1.1.0]
### Added
- **State Commands**: Added `/bud [attack|atk]`, `/bud [follow|fol]`, and `/bud [chill|stay]`.
- **Reset Command**: Added `/bud reset` to cleanup and respawn all buddies.

### Fixed
- Increased default `MaxTokens` from 30 to 400 for better LLM answers.
- Added position offset for spawned and teleported Buds.
- Switched from `System.out.println` to proper `LoggerUtil` logging.
- Removed unused `LLMCommand` class.
