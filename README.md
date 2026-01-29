# JokeLLM Plugin

Used template by [Up](https://github.com/UpcraftLP), and slightly modified by Kaupenjoe. 

## LLMJokePlugin

This is like a small PoC to create a Hytale Plugin with integrated LLM calls. Maybe for further work, this could be used for RPG like mechanics. It would be possible to create NPC buddies who could communicate with the player via LLM. Companions (orcs, trolls, etc.) could spout silly lines and, through LLM, feel more alive. Local "memories" for each companion would also be conceivable.

## Quickstart

Move **LLMJokePlugin-0.1.0.jar** to Mods folder. Enable this Mod for a world. Afterwards, edit the LLMJoke.json in your *Saves/worldname/mods/LLMGroup_LLMJokePlugin/* Folder.

**Important:** You need API connection data for your own LLM. In this example, I used *ibm/granite-4-h-tiny* and *mistralai/ministral-3-3b* locally, so no API key was needed.

### LLMJoke.json

Values to set:
- Url: The URL to your LLM (E.g. LM Studio: http://192.168.178.25:1234/v1/chat/completions)
- Model: The model API identifier (E.g. ibm/granite-4-h-tiny)
- ApiKey: If needed, your external API key

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
# /joke in chat
