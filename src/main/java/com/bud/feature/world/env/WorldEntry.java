package com.bud.feature.world.env;

import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.types.TimeOfDay;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.time.TimeInformationUtil;
import com.bud.feature.world.time.TimeMessage;
import com.bud.feature.world.weather.WeatherEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public record WorldEntry(@Nonnull TimeOfDay timeOfDay, @Nonnull Zone currentZone, @Nonnull Biome currentBiome,
        @Nonnull WeatherEntry weatherEntry, @Nonnull BudComponent budComponent) implements IQueueEntry {

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    public static WorldEntry from(@Nonnull PlayerRef owner, @Nonnull World world,
            @Nonnull Store<EntityStore> store, @Nonnull WeatherEntry weatherEntry, @Nonnull BudComponent budComponent) {
        Vector3d pos = owner.getTransform().getPosition();
        TimeOfDay timeOfDay = TimeInformationUtil.getTimeOfDay(store);
        LoggerUtil.getLogger().fine(() -> "[BUD] time of day: " + timeOfDay.name());
        Zone zone = WorldInformationUtil.getCurrentZone(world, pos);
        if (zone == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not determine current zone for player: "
                    + owner.getUsername());
            return null;
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] current zone: " + zone.name());
        Biome biome = WorldInformationUtil.getCurrentBiome(world, pos);
        if (biome == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not determine current biome for player: "
                    + owner.getUsername());
            return null;
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] current biome: " + biome.getName());
        return new WorldEntry(timeOfDay, zone, biome, weatherEntry, budComponent);
    }

    public ZoneMessage getZoneInfo(LLMPromptManager manager) {
        String zoneName = this.currentZone.name().toLowerCase();
        if (zoneName.contains("zone1") || zoneName.contains("emerald")) {
            return manager.getZoneMessage("emerald_grove");
        }
        if (zoneName.contains("zone2") || zoneName.contains("howling")) {
            return manager.getZoneMessage("howling_sands");
        }
        if (zoneName.contains("zone3") || zoneName.contains("whisperfrost")) {
            return manager.getZoneMessage("whisperfrost_frontiers");
        }
        if (zoneName.contains("zone4") || zoneName.contains("devastated")) {
            return manager.getZoneMessage("devastated_lands");
        }
        if (zoneName.contains("zone0")) {
            return manager.getZoneMessage("ocean");
        }

        return manager.getZoneMessage("fallback");
    }

    public String getBiomeInfo(ZoneMessage zoneMessage) {
        String biomeName = this.currentBiome.getName();
        return zoneMessage.getBiomes().entrySet().stream()
                .filter(e -> biomeName.toLowerCase().contains(e.getKey().toLowerCase()))
                .map(Entry::getValue)
                .findFirst()
                .orElseGet(() -> zoneMessage.getBiomes().getOrDefault("default", biomeName));
    }

    public String getTimeInfo(TimeMessage timeMsg) {
        String timeInfo = "Unknown Time";
        if (timeMsg != null && timeMsg.getTimes() != null) {
            timeInfo = timeMsg.getTimes().entrySet().stream()
                    .filter(e -> this.timeOfDay.name().toLowerCase().contains(e.getKey().toLowerCase()))
                    .map(Entry::getValue)
                    .findFirst()
                    .orElse(this.timeOfDay.name());
        }
        return timeInfo;
    }

    public String getWeatherInfo() {
        return this.weatherEntry.getWeatherInformation();
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        String zoneName = currentZone.name();
        if (zoneName == null || zoneName.isEmpty()) {
            return "Unknown Zone";
        }
        return zoneName;
    }
}
