package com.bud.llm.message.world;

import java.util.Map.Entry;

import com.bud.data.TimeOfDay;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.llm.message.prompt.TimeMessage;
import com.bud.llm.message.prompt.ZoneMessage;
import com.bud.util.TimeInformationUtil;
import com.bud.util.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public record LLMWorldContext(TimeOfDay timeOfDay, Zone currentZone, Biome currentBiome)
        implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        // Implement this method based on your context retrieval logic
        return switch (contextId) {
            case "timeOfDay" -> timeOfDay.name();
            case "currentZone" -> currentZone.name();
            case "currentBiome" -> currentBiome.getName();
            default -> null;
        };
    }

    public static LLMWorldContext from(PlayerRef owner, World world, Store<EntityStore> store) {
        Vector3d pos = owner.getTransform().getPosition();
        TimeOfDay tod = TimeInformationUtil.getTimeOfDay(store);
        LoggerUtil.getLogger().fine(() -> "[BUD] time of day: " + tod.name());
        Biome biome = WorldInformationUtil.getCurrentBiome(world, pos);
        LoggerUtil.getLogger().fine(() -> "[BUD] current biome: " + biome.getName());
        Zone zone = WorldInformationUtil.getCurrentZone(world, pos);
        LoggerUtil.getLogger().fine(() -> "[BUD] current zone: " + zone.name());
        return new LLMWorldContext(tod, zone, biome);
    }

    public ZoneMessage getZoneInfo(LLMPromptManager manager) {

        String zoneName = this.currentZone.name().toLowerCase();
        if (zoneName.contains("1") || zoneName.contains("emerald"))
            return manager.getZoneMessage("emerald_grove");
        if (zoneName.contains("2") || zoneName.contains("howling"))
            return manager.getZoneMessage("howling_sands");
        if (zoneName.contains("3") || zoneName.contains("whisperfrost"))
            return manager.getZoneMessage("whisperfrost_frontiers");
        if (zoneName.contains("4") || zoneName.contains("devastated"))
            return manager.getZoneMessage("devasted_lands");
        if (zoneName.contains("ocean"))
            return manager.getZoneMessage("ocean");
        if (zoneName.contains("dungeon"))
            return manager.getZoneMessage("dungeons");

        return manager.getZoneMessage("fallback");
    }

    public String getBiomeInfo(ZoneMessage zoneMessage) {
        String biomeName = this.currentBiome().getName();
        // Try to find the biome in the map (case-insensitive key search)
        String biomeInfo = zoneMessage.getBiomes().entrySet().stream()
                .filter(e -> biomeName.toLowerCase().contains(e.getKey().toLowerCase()))
                .map(Entry::getValue)
                .findFirst()
                .orElseGet(() -> {
                    // Default backup logic for biomes
                    return zoneMessage.getBiomes().getOrDefault("default", biomeName);
                });
        return biomeInfo;
    }

    public String getTimeInfo(TimeMessage timeMsg) {
        String timeInfo = "Unknown Time";
        if (timeMsg != null && timeMsg.getTimes() != null) {
            timeInfo = timeMsg.getTimes().entrySet().stream()
                    .filter(e -> this.timeOfDay().name().toLowerCase().contains(e.getKey().toLowerCase()))
                    .map(Entry::getValue)
                    .findFirst()
                    .orElse(this.timeOfDay().name());
        }
        return timeInfo;
    }
}