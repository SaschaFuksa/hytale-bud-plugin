# Hytale Bud Plugin

Used template by [Up](https://github.com/UpcraftLP), and slightly modified by [Kaupenjoe](https://github.com/Kaupenjoe).
Also inspired by [MyFriends](https://www.curseforge.com/hytale/mods/my-friends) from [LukeysMods](https://www.curseforge.com/members/lukeysmods/projects), used some of the configs like particles.

## Side Notes

This is my first Mod ever, so be kind to me. Also, Hytale Modding is still in early development, so things might change in the future.
This mod will also work without LLM, but the main goal is to have interactive buddies.

## Bud Plugin

This is like a small proof of concept (PoC) to create a Hytale Plugin with integrated LLM calls. The goal is to bring more RPG like buddies into Hytale, which can interact with the player via LLM.
Currently there are three Buds implemented:
- **Veri**: A Feran buddy. It is a little bit childish, but very curious and friendly. It supports the player with daggers.
- **Gronkh**: A Trork buddy. He is a little bit grumpy, but very loyal and strong. And yes, we all love his streams. He supports the player with a mace.
- **Keyleth**: A Kweebec buddy. She is very smart, but also a little bit shy. She supports the player with a bow.

## Features 1.0.0

This mod will be controlled via commands:
- **/bud**: The initial command to spawn all three buddies around you. It also respawns them if they died or teleports existing ones to you.
- **/bud Veri|Gronkh|Keyleth**: Spawn a specific buddy around you or teleports existing one to you.
- **/bud clean**: Remove all your buddies.
- **/bud clean-all**: Remove all buddies in the world.
- **/bud data**: Prints uuids of your current persisted buddies.
- **/bud data-clean**: Remove all persisted data (Bud UUIDs).

Hint: Any other word after /bud will print the "help" message. All possible commands will be listed there.

Interaction with buddies:
- Press "F" to interact with your targeted buddy to bind them to you.
- "F" will also switch between three different interaction modes:
  - **Pet Defensive Mode**: The buddy will follow you around and attacks enemies.
  - **Pet Passive Mode**: The buddy will only follow you around.
  - **Pet Sitting Mode**: The buddy will stay at the current position.
- Initial status is **Pet Defensive Mode**.

Automatic despawning of buddies:
- If player disconnects, all of his buddies will despawn.
- If player login, all of his buddies will despawn. This is needed, if server disconnects while player is online.

Custom Chat Interaction (Only with LLM enabled):
- Your Bud will sends a chat message with current world informations every three minutes.
- Your Bud will sends a chat message in reference to the last combats you had.

## Changelog

### 1.2.2 Hot Bugfixes
- Fixed issue with shadow jar build where Jackson classes were not found at runtime.
- Also fixed issue with permissions, I accidentally removed a needed override method. Now, also non admin players can use the /bud commands.

### 1.2.0 Features
Goal: More control over LLM behavior via config.

New config options:
- Add MaxTokens config option to limit token usage.
- Add Temperature config option to control randomness of LLM responses.
- Add UsePlayer2API config option to switch between normal LLM calls and Player2 API calls.
Also implemented Player2 API to make AI-integration more easy. But I don't implemented chat reactions to player messages yet. Maybe a future update.

### 1.2.0 Bugfixes:
- Code improvements, espacially for sync/async methods.
- Bud sound will now play after message was sent
- Changed combat chat message system from polling to event-driven:
  - Removed polling with 10 seconds over all players
  - Now send message after combat interaction with 2 seconds delay.
  - Also added cooldown of 3 seconds befor shedule next combat message.

### 1.1.0 Feature
Goal: Command to toggle state for all Buds like "/bud attack"

New commands:
- **/bud [attack|atk]**: Sets all your buddies to Defensive Mode.
- **/bud [follow|fol]**: Sets all your buddies to Passive Mode.
- **/bud [chill|stay]**: Sets all your buddies to Sitting Mode.
- **/bud reset**: Remove and respawn all your buddies.

### 1.1.0 Bugfixes
- Set token amount to 400 instead of 30 for better answers from LLM.
- Added offset to spawned and teleported Buds position.
- Logger based prints instead of System.out.println for better visibility in server logs.
- Removed unused LLMCommand class.

## Quickstart

Move **HytaleBudPlugin-x.x.x.zip** to your global Mods folder. Enable this Mod for a world. Afterwards, edit the HytaleBud.json in your *Saves/worldname/mods/HytaleBudPlugin/* folder to enable LLM.

**Important**: You need API connection data for your own LLM. In this example, I used *ibm/granite-4-h-tiny* and *mistralai/ministral-3-3b* locally, so no API key was needed. Both worked fine for testing.

## Requirements LLM

- Self Hosting: With ministral-3-3b, I needed at least ~9GB VRAM for smooth operation (Hytale included).
- External Hosting: You can also use external LLM providers. Make sure to check their pricing and API limits.

### Bud.json

Values to set:
- **EnableLLM**: true/false to enable/disable LLM usage (Default: "true")
- **UsePlayer2API**: true/false to use Player2 API for LLM calls (Default: "false", needs EnableLLM true)
- **Url**: The URL to your LLM (E.g. LM Studio: "http://192.168.178.25:1234/v1/chat/completions")
- **Model**: The model API identifier (E.g. "ibm/granite-4-h-tiny")
- **ApiKey**: If needed, your external API key (Default: "not_needed")
- **MaxTokens**: Maximum tokens to use per LLM call (Default: 400)
- **Temperature**: Temperature for LLM responses (Default: 0.8)

## Use of this repo in your IDE
If you for example installed the game in a non-standard location, you will need to tell the project about that.
The recommended way is to create a file at `%USERPROFILE%/.gradle/gradle.properties` to set these properties globally.

```properties
# Set a custom game install location
hytale.install_dir=path/to/Hytale

# Speed up the decompilation process significantly, by only including the core hytale packages.
# Recommended if decompiling the game takes a very long time on your PC.
hytale.decompile_partial=true

# Dev commands:
Initial: .\gradlew decompileServer

build: .\gradlew build
start server: .\gradlew runServer
/auth login device
/auth persistence Encrypted

# ingame: direct connect -> localhost
# /bud in chat
```

## Roadmap
- 1.3.0: Seperate prompts from code for easier editing.
Look for a solution.
-> SnakeYaml seems to be a good option, I cann add it to the plugin and the size increase is minimal.
Bug Fixes:
- Current mods folder now renamed to "BudPlugin" instead og "Bud_  BudPLugin"
- Max token size reduced to max. 200 for better performance and less token usage.
- Also add now config data (max tokens and temperature) to Player2 call
- Now log consumed tokens

- 1.4.0: Minor memory storage like last mined blocks, etc. to react better to world (similar like current combat memory).
- 1.5.0: Update of Bud models, add at least one special appearance per Bud.
- 1.6.0: Add items to spawn Buds instead of commands.
- 1.7.0: [User feedback feature]
- 1.8.0: [User feedback feature]
- 1.9.0: Add magic Bud with spells.
- 2.0.0: PoC for Bud interaction with world via LLM.

## Sidequests
- Maybe one more state with particles (Idle?).
- More story telling for Buds.
- Better combat memory handling.
- Optimize LLM calls to reduce token usage.

## Known Issues
- Sometimes after teleport, Buds get stuck and won't teleport. Workaround: Clean-all and respawn them.
