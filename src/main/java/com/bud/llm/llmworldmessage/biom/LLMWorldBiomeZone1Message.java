package com.bud.llm.llmworldmessage.biom;

import com.bud.llm.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeZone1Message implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String biomeName = context.currentBiome().getName().toLowerCase();

        if (biomeName.contains("plains") || biomeName.contains("fields")) {
            return "There are pretty plains full of animals and foliage.";
        } else if (biomeName.contains("forest") || biomeName.contains("woods")) {
            if (biomeName.contains("azure")) {
                return "There are beautiful and magical blue plants and forest. The magical Azure Kelp flower can be found in the lakes here.";
            } else if (biomeName.contains("autumn")) {
                return "There are beautiful autumn forests with red and colorful foliage. There are many mushrooms, including the rare mystical Bloodcap Mushroom.";
            }
            return "There are dense forests with diverse wildlife. But keep an eye out for hidden dangers like brown bears.";
        } else if (biomeName.contains("swamp")) {
            return "A dark and wet biome, the Swamps and Marshlands of Orbis can be discovered here. But there are dangerous creatures: Fen stalkers.";
        } else if (biomeName.contains("cave") || biomeName.contains("tunnels")) {
            return "The deep caves and tunnels are filled with copper ore and iron ore. But be careful of monsters and cave spiders.";
        } else {
            return "The current biome is " + biomeName + ".";
        }
    }
}
