package com.bud.system;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.hypixel.hytale.math.vector.Vector3d;

public class BudWorldInformation {

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
    
}
