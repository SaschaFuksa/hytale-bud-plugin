# Hytale Bud Plugin

Used template by [Up](https://github.com/UpcraftLP), and slightly modified by [Kaupenjoe](https://github.com/Kaupenjoe).
Also inspired by [MyFriends](https://www.curseforge.com/hytale/mods/my-friends) from [LukeysMods](https://www.curseforge.com/members/lukeysmods/projects), used some of the configs like particles.

## Side Notes

This is my first Mod ever, so be kind to me. Also, Hytale Modding is still in early development, so things might change in the future.
This mod will also work without LLM, but the main goal is to have interactive buddies.

## Bud Plugin

This is like a small PoC to create a Hytale Plugin with integrated LLM calls. The goal is to bring more RPG like buddies into Hytale, which can interact with the player via LLM.
Currently there are three Buds implemented:
- **Veri**: A Feran buddy. It is a little bit childish, but very curious and friendly. It supports the player with daggers.
- **Gronkh**: A Trork buddy. He is a little bit grumpy, but very loyal and strong. And yes, we all love his streams. He supports the player with a mace.
- **Kacche**: A Kweebec buddy. She is very smart, but also a little bit shy. She supports the player with a bow.

## Features

This mod will be controlled via commands:
- **/bud**: The initial command to spawn all three buddies around you. It also respawns them, if they died and teleports all to you.
- **/bud clear**: Remove all your buddies.
- **/bud clear-all**: Remove all buddies in the world.

Interaction with buddies:
- Press "F" to interact with your targeted buddy to bind them to you.
- "F" will also switch between three different interaction modes:
  - **Attack Mode**: The buddy will follow you around and attacks enemies.
  - **Passive Mode**: The buddy will only follow you around.
  - **Idle Mode**: The buddy will stay at the current position.

## Quickstart

Move **HytaleBudPlugin-x.x.x.zip** to your global Mods folder. Enable this Mod for a world. Afterwards, edit the HytaleBud.json in your *Saves/worldname/mods/HytaleBudPlugin/* folder to enable LLM.

**Important**: You need API connection data for your own LLM. In this example, I used *ibm/granite-4-h-tiny* and *mistralai/ministral-3-3b* locally, so no API key was needed. Both worked fine for testing.

### Bud.json

Values to set:
- **EnableLLM**: true/false to enable/disable LLM usage (Default: "false")
- **Url**: The URL to your LLM (E.g. LM Studio: "http://192.168.178.25:1234/v1/chat/completions")
- **Model**: The model API identifier (E.g. "ibm/granite-4-h-tiny")
- **ApiKey**: If needed, your external API key (Default: "not_needed")

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
