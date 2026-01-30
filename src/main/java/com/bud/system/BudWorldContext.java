package com.bud.system;

import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public record BudWorldContext(TimeOfDay timeOfDay, Zone currentZone, Biome currentBiome) {

}
