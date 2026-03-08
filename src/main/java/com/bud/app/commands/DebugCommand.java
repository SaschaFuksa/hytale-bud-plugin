package com.bud.app.commands;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.DayOfWeek;
import com.bud.core.types.TimeOfDay;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.time.TimeInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public class DebugCommand extends AbstractPlayerCommand {

    private final FlagArg componentDataFlag;
    private final FlagArg moodFlag;
    private final FlagArg weatherFlag;
    private final FlagArg timeFlag;
    private final FlagArg worldFlag;

    public DebugCommand() {
        super("debug", "Debug command for testing purposes.");
        this.componentDataFlag = this.withFlagArg("componentData",
                "Shows the current persisted data for the player's Buds.");
        this.moodFlag = this.withFlagArg("mood", "Shows the mood of the current Buds and their favorite day.");
        this.weatherFlag = this.withFlagArg("weather", "Shows the current weather.");
        this.timeFlag = this.withFlagArg("time", "Shows the current time of day and day of week.");
        this.worldFlag = this.withFlagArg("world", "Shows the current zone and biome.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        boolean handled = false;

        if (this.componentDataFlag.get(context)) {
            handled = true;
            LoggerUtil.getLogger().fine(() -> "[BUD] Component debug command executed for player "
                    + playerRef.getUsername());
            this.sendComponentData(store, playerRef);
        }
        if (this.moodFlag.get(context)) {
            handled = true;
            this.sendMoodData(store, playerRef);
        }
        if (this.weatherFlag.get(context)) {
            handled = true;
            this.sendWeatherData(playerRef);
        }
        if (this.timeFlag.get(context)) {
            handled = true;
            this.sendTimeData(store, world, playerRef);
        }
        if (this.worldFlag.get(context)) {
            handled = true;
            this.sendWorldData(world, playerRef);
        }

        if (!handled) {
            ChatEvent.dispatch(playerRef, "Debug flags: --componentData, --mood, --weather, --time, --world");
        }
    }

    private void sendComponentData(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        PlayerBudComponent playerBudComponent = getPlayerBudComponent(store, playerRef);
        if (playerBudComponent == null) {
            ChatEvent.dispatch(playerRef, "ComponentData: no PlayerBudComponent found.");
            return;
        }

        if (playerBudComponent.getCurrentBuds().isEmpty() && playerBudComponent.getBudTypes().isEmpty()) {
            ChatEvent.dispatch(playerRef, "ComponentData: no Bud data found.");
            return;
        }

        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            ChatEvent.dispatch(playerRef, "ComponentData current: " + bud.getNPCTypeId());
        }
        playerBudComponent.getBudTypes().forEach(budType -> ChatEvent.dispatch(playerRef,
                "ComponentData persisted: " + budType.getName()));
    }

    private void sendMoodData(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        PlayerBudComponent playerBudComponent = getPlayerBudComponent(store, playerRef);
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            ChatEvent.dispatch(playerRef, "Mood: no active Buds found.");
            return;
        }

        List<String> lines = new ArrayList<>();
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
            if (budComponent == null) {
                continue;
            }
            String budDisplayName = BudProfileMapper.getInstance()
                    .getProfileForBudType(budComponent.getBudType())
                    .getNPCDisplayName();
            DayOfWeek favoriteDay = BudProfileMapper.getInstance()
                    .getProfileForBudType(budComponent.getBudType())
                    .getFavoriteDay();
            lines.add("Mood " + budDisplayName + ": " + formatDisplayValue(budComponent.getCurrentMood().name())
                    + " (Favorite Day: " + formatDisplayValue(favoriteDay.name()) + ")");
        }

        if (lines.isEmpty()) {
            ChatEvent.dispatch(playerRef, "Mood: no active Buds found.");
            return;
        }

        for (String line : lines) {
            if (line == null) {
                continue;
            }
            ChatEvent.dispatch(playerRef, line);
        }
    }

    private void sendWeatherData(@Nonnull PlayerRef playerRef) {
        Weather weather = WorldInformationUtil.getCurrentWeather(playerRef);
        if (weather == null || weather.getId() == null || weather.getId().isBlank()) {
            ChatEvent.dispatch(playerRef, "Weather: Unknown");
            return;
        }
        ChatEvent.dispatch(playerRef, "Weather: " + formatDisplayValue(weather.getId()));
    }

    private void sendTimeData(@Nonnull Store<EntityStore> store, @Nonnull World world, @Nonnull PlayerRef playerRef) {
        TimeOfDay timeOfDay = TimeInformationUtil.getTimeOfDay(store);
        DayOfWeek dayOfWeek = TimeInformationUtil.getDayOfWeek(world);
        ChatEvent.dispatch(playerRef, "Time: " + formatDisplayValue(timeOfDay.name())
                + ", Day: " + formatDisplayValue(dayOfWeek.name()));
    }

    private void sendWorldData(@Nonnull World world, @Nonnull PlayerRef playerRef) {
        Vector3d position = playerRef.getTransform().getPosition();
        Zone zone = WorldInformationUtil.getCurrentZone(world, position);
        Biome biome = WorldInformationUtil.getCurrentBiome(world, position);

        String zoneName = zone != null ? formatDisplayValue(zone.name()) : "Unknown";
        String biomeName = biome != null ? formatDisplayValue(biome.getName()) : "Unknown";
        ChatEvent.dispatch(playerRef, "World: Zone " + zoneName + ", Biome " + biomeName);
    }

    private PlayerBudComponent getPlayerBudComponent(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        Ref<EntityStore> playerRefReference = playerRef.getReference();
        if (playerRefReference == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] PlayerRef reference is null for player " + playerRef.getUsername());
            return null;
        }

        PlayerBudComponent playerBudComponent = store.getComponent(playerRefReference,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] No PlayerBudComponent found for player "
                    + playerRef.getUsername());
        }
        return playerBudComponent;
    }

    private String formatDisplayValue(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        String normalized = value;
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        normalized = normalized.replace('-', '_');

        String[] parts = normalized.split("[_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.length() > 0 ? builder.toString() : value;
    }

}
