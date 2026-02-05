# Changelog

All notable changes to this project will be documented in this file.

## [1.3.0]
### Added
- **Ticker-based Spawning**: Buds now spawn with a 0.3s delay between each other.
- **Spawn range**: Make it more random by adding a small random offset to the spawn location of each Bud.
- **YAML Prompt Management**: Refactored logic to use external `prompt.yml` files for easier editing of NPC personalities.
- **Token Logging**: Consumed tokens are now logged for every LLM interaction.
- **Synchronized NPC Lists**: Spawning results are now handled in a thread-safe list to provide immediate feedback while work continues in the background.

### Fixed
- **State Mapping**: Corrected name of mod folder to "Bud_Plugin"
- **Misconfig**: Now handle better enabled LLM state with no activ LLM instance. Make sounds/chat more robust.

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
- **Audio feedback**: Bud sounds now play after a message is sent.

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
