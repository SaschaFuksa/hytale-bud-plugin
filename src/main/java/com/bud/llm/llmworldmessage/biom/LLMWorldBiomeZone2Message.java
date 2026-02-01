package com.bud.llm.llmworldmessage.biom;

import com.bud.llm.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeZone2Message implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String biomeName = context.currentBiome().getName().toLowerCase();

        if (biomeName.contains("badlands")) {
            return "There are dry, red rocky plains with steep canyons. It is hot and few animals live here.";
        } else if (biomeName.contains("steppes") || biomeName.contains("savanna")) {
            return "There are wide open steppes and savannas with scattered trees and grasslands. Keep an eye out for roaming lions and hyenas.";
        } else if (biomeName.contains("desert")) {
            return "The desert is hot and arid, with vast dunes and bone sculptures. Watch out for sandstorms and keep an eye on dangerous cacti.";
        } else if (biomeName.contains("hot spring") || biomeName.contains("oasis")) {
            return "A wonderful place with warm waters and lush vegetation. Time to relax, enjoy the soothing environment.";
        } else if (biomeName.contains("cave") || biomeName.contains("tunnels")) {
            return "The deep caves and tunnels are filled with iron ore and thorium ore. But be careful of monsters and cave spiders.";
        } else {
            return "The current biome is " + biomeName + ".";
        }
    }
}
