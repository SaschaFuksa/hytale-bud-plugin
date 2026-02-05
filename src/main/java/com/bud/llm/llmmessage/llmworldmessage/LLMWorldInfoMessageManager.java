package com.bud.llm.llmmessage.llmworldmessage;

import java.util.Map.Entry;

import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.llm.llmmessage.BudLLMPromptManager;
import com.bud.llm.llmmessage.TimeLLMMessage;
import com.bud.llm.llmmessage.WorldInfoTemplateMessage;
import com.bud.llm.llmmessage.ZoneLLMMessage;
import com.bud.system.BudWorldContext;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMWorldInfoMessageManager {

    public static String createPrompt(BudWorldContext context, BudLLMMessage npcMessage) {
        if (npcMessage == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] npcMessage is null in createPrompt!");
            return "No NPC message available.";
        }
        BudLLMPromptManager manager = BudLLMPromptManager.getInstance();
        WorldInfoTemplateMessage template = manager.getWorldInfoTemplate();

        String zoneKey = getZoneKey(context.currentZone().name());
        ZoneLLMMessage zoneMessage = manager.getZoneMessage(zoneKey);

        String zoneInfo = (zoneMessage != null) ? zoneMessage.getZone() : "Unknown Zone";
        String biomeInfo = "Unknown Biome";
        if (zoneMessage != null && zoneMessage.getBiomes() != null) {
            String biomeName = context.currentBiome().getName();
            // Try to find the biome in the map (case-insensitive key search)
            biomeInfo = zoneMessage.getBiomes().entrySet().stream()
                    .filter(e -> biomeName.toLowerCase().contains(e.getKey().toLowerCase()))
                    .map(Entry::getValue)
                    .findFirst()
                    .orElseGet(() -> {
                        // Default backup logic for biomes
                        return zoneMessage.getBiomes().getOrDefault("default", "Biome: " + biomeName);
                    });
        }

        TimeLLMMessage timeMsg = manager.getTimeMessage();
        String timeInfo = "Unknown Time";
        if (timeMsg != null && timeMsg.getTimes() != null) {
            String timeOfDay = context.timeOfDay().name();
            timeInfo = timeMsg.getTimes().entrySet().stream()
                    .filter(e -> timeOfDay.toLowerCase().contains(e.getKey().toLowerCase()))
                    .map(Entry::getValue)
                    .findFirst()
                    .orElse("Time: " + timeOfDay);
        }

        String budInfo = npcMessage.getSystemPrompt();
        String introduction = template.getIntroduction();
        String environmentInfo = template.getEnvironmentInfo().formatted(zoneInfo, biomeInfo, timeInfo);
        String budInfo2 = npcMessage.getPersonalWorldView();

        return budInfo + "\n" + introduction + "\n" + environmentInfo + "\n" + budInfo2;
    }

    private static String getZoneKey(String zoneName) {
        zoneName = zoneName.toLowerCase();
        if (zoneName.contains("1") || zoneName.contains("emerald"))
            return "emerald_grove";
        if (zoneName.contains("2") || zoneName.contains("howling"))
            return "howling_sands";
        if (zoneName.contains("3") || zoneName.contains("whisperfrost"))
            return "whisperfrost_frontiers";
        if (zoneName.contains("4") || zoneName.contains("devastated"))
            return "devasted_lands";
        if (zoneName.contains("ocean"))
            return "ocean";
        if (zoneName.contains("dungeon"))
            return "dungeons";
        return zoneName;
    }

}
