package com.bud.feature.world.env;

import java.util.Map.Entry;

import com.bud.core.components.BudComponent;
import com.bud.llm.messages.weather.LLMWeatherContext;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.feature.profile.IBudProfile;
import com.bud.feature.reaction.world.time.TimeInformationUtil;
import com.bud.feature.reaction.world.time.TimeOfDay;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.time.TimeMessage;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public record LLMWorldContext(TimeOfDay timeOfDay, Zone currentZone, Biome currentBiome,
        LLMWeatherContext weatherContext)
        implements IPromptContext {

    public static LLMWorldContext from(PlayerRef owner, World world,
            Store<EntityStore> store, LLMWeatherContext weatherContext) {
        Vector3d pos = owner.getTransform().getPosition();
        TimeOfDay timeOfDay = TimeInformationUtil.getTimeOfDay(store);
        LoggerUtil.getLogger().fine(() -> "[BUD] time of day: " + timeOfDay.name());
        Biome biome = WorldInformationUtil.getCurrentBiome(world, pos);
        LoggerUtil.getLogger().fine(() -> "[BUD] current biome: " + biome.getName());
        Zone zone = WorldInformationUtil.getCurrentZone(world, pos);
        LoggerUtil.getLogger().fine(() -> "[BUD] current zone: " + zone.name());
        return new LLMWorldContext(timeOfDay, zone, biome, weatherContext);
    }

    public ZoneMessage getZoneInfo(LLMPromptManager manager) {

        String zoneName = this.currentZone.name().toLowerCase();
        if (zoneName.contains("zone1") || zoneName.contains("emerald"))
            return manager.getZoneMessage("emerald_grove");
        if (zoneName.contains("zone2") || zoneName.contains("howling"))
            return manager.getZoneMessage("howling_sands");
        if (zoneName.contains("zone3") || zoneName.contains("whisperfrost"))
            return manager.getZoneMessage("whisperfrost_frontiers");
        if (zoneName.contains("zone4") || zoneName.contains("devastated"))
            return manager.getZoneMessage("devastated_lands");
        if (zoneName.contains("zone0"))
            return manager.getZoneMessage("ocean");

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

    public String getWeatherInfo() {
        return this.weatherContext.getWeatherInformation();
    }

    @Override
    public BudComponent getBudComponent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudComponent'");
    }

    @Override
    public IBudProfile getBudProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudProfile'");
    }
}