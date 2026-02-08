package com.bud.util;

import com.bud.npc.BudInstance;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public class WorldInformationUtil {

    public static Zone getCurrentZone(World world, Vector3d pos) {
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (worldGen instanceof ChunkGenerator generator) {
            int seed = (int) world.getWorldConfig().getSeed();
            int x = (int) pos.getX();
            int z = (int) pos.getZ();

            try {
                ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
                if (result != null) {
                    return result.getZoneResult().getZone();
                }
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Error getting zone at position: " + e.getMessage());
            }
        }
        return null;
    }

    public static Biome getCurrentBiome(World world, Vector3d pos) {
        IWorldGen worldGen = world.getChunkStore().getGenerator();
        if (worldGen instanceof ChunkGenerator generator) {
            int seed = (int) world.getWorldConfig().getSeed();
            int x = (int) pos.getX();
            int z = (int) pos.getZ();

            try {
                ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
                if (result != null) {
                    return result.getBiome();
                }
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Error getting biome at position: " + e.getMessage());
            }
        }
        return null;
    }

    public static World resolveWorld(BudInstance budInstance) {
        // Try entity world
        World world = budInstance.getEntity().getWorld();
        if (world != null) {
            return world;
        }

        // Try owner world
        PlayerRef owner = budInstance.getOwner();
        if (owner != null) {
            Ref<EntityStore> ownerRef = owner.getReference();
            if (ownerRef != null) {
                Store<EntityStore> store = ownerRef.getStore();
                return store.getExternalData().getWorld();
            }
        }

        return null;
    }

    public static Weather getCurrentWeather(BudInstance budInstance) {
        World world = resolveWorld(budInstance);
        Ref<EntityStore> ownerRef = budInstance.getOwner().getReference();
        ComponentType<EntityStore, WeatherTracker> componentType = WeatherTracker.getComponentType();
        if (world == null || ownerRef == null || componentType == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Unable to resolve world or owner for BudInstance.");
            return null;
        }
        WeatherTracker tracker = world.getEntityStore().getStore().getComponent(ownerRef,
                componentType);
        int index = tracker.getWeatherIndex();
        Weather weather = Weather.getAssetMap().getAsset(index);
        LoggerUtil.getLogger().info(() -> "[BUD] Current weather: " + weather);
        return weather;
    }

}
