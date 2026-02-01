package com.bud.llm.llmworldmessage.biom;

import com.bud.llm.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeZone3Message implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String biomeName = context.currentBiome().getName().toLowerCase();

        if (biomeName.contains("tundra")) {
            return "The tundra is cold and barren, with sparse vegetation and a harsh climate.";
        } else if (biomeName.contains("boreal")) {
            return "Snowy forest and frozen ground are dotted with pine trees. At night, many undead are roaming through the trees.";
        } else if (biomeName.contains("everfrost")) {
            return "The everfrost is a frozen wasteland with icy winds and treacherous terrain. Watch out for frostbite and blizzards.";
        } else if (biomeName.contains("mountain")) {
            return "Huge mountains with rocky peaks and steep cliffs. The air is thin and the weather can be unpredictable. The dangerous Yeti lives here.";
        } else if (biomeName.contains("glacier")) {
            return "Big glaciers with vast ice formations and crevasses. The cold is intense and the terrain is challenging. Dangerous snowstorms, polar bears and ice wanderers are common.";
        } else if (biomeName.contains("ice")) {
            return "Icebergs drift through the frigid waters, creating a stunning but perilous landscape. Say hello to the penguins and watch out for polar bears.";
        } else if (biomeName.contains("cave") || biomeName.contains("tunnels")) {
            return "The deep caves and tunnels are filled with iron ore and cobalt ore. But be careful of monsters and cave spiders. Also watch out for ice lakes and frozen traps.";
        } else {
            return "The current biome is " + biomeName + ".";
        }
    }
}
