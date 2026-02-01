package com.bud.llm.llmworldmessage.biom;

import com.bud.llm.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeZone4Message implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String biomeName = context.currentBiome().getName().toLowerCase();

        if (biomeName.contains("charred")) {
            return "Dark, gloomy, swampy and burnt places everywhere. Watch out for undead monsters.";
        } else if (biomeName.contains("volcano") || biomeName.contains("magma")) {
            return "Hot and fiery with flowing lava. Big gaps are everywhere. Watch out for undead monsters.";
        } else if (biomeName.contains("everburning")) {
            return "A forest that is perpetually on fire, with flames that never die out. Much smoke and everything seems to be dead. But undead monsters are roaming here.";
        } else if (biomeName.contains("ash")) {
            return "A white and grey landscape covered in ash and soot.";
        } else if (biomeName.contains("mushroom")) {
            return "Glooming mushroom forests with strange fungi and eerie atmosphere.";
        } else if (biomeName.contains("desolated")) {
            return "Desolated areas with barren land and little life.";
        } else if (biomeName.contains("tropical") || biomeName.contains("jungle")) {
            return "A wonderful tropical jungle with lush vegetation and diverse wildlife. Glowing crystals bringing light in the underground jungle. But be careful of dinosaurs roaming around.";
        } else if (biomeName.contains("cave") || biomeName.contains("tunnels")) {
            return "The deep caves and tunnels are filled with iron ore and adamantite ore. But be careful of monsters and cave spiders. Also watch out for lava lakes and fire traps.";
        } else {
            return "The current biome is " + biomeName + ".";
        }
    }
}
