package com.bud.llm.message.discover;

import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.llm.message.prompt.ZoneMessage;
import com.bud.reaction.discover.DiscoverEntry;

/**
 * Context for discover zone LLM prompts.
 * Resolves zone/region names to existing zone descriptions.
 */
public record LLMDiscoverContext(DiscoverEntry discoverEntry) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        return switch (contextId) {
            case "zoneName" -> this.discoverEntry.zoneName();
            case "regionName" -> this.discoverEntry.regionName();
            case "major" -> String.valueOf(this.discoverEntry.major());
            default -> null;
        };
    }

    public static LLMDiscoverContext from(DiscoverEntry entry) {
        return new LLMDiscoverContext(entry);
    }

    /**
     * Attempts to match the regionName (e.g. "Zone1_Shore") to an existing zone
     * description.
     * Uses the same mapping logic as LLMWorldContext.getZoneInfo().
     */
    public ZoneMessage getZoneInfo(LLMPromptManager manager) {
        String regionLower = this.discoverEntry.regionName().toLowerCase();
        String zoneLower = this.discoverEntry.zoneName().toLowerCase();

        // Try matching by region name first (e.g. "Zone1_Shore" → emerald_grove)
        if (regionLower.contains("zone1") || zoneLower.contains("emerald")) {
            return manager.getZoneMessage("emerald_grove");
        }
        if (regionLower.contains("zone2") || zoneLower.contains("howling")) {
            return manager.getZoneMessage("howling_sands");
        }
        if (regionLower.contains("zone3") || zoneLower.contains("whisperfrost")) {
            return manager.getZoneMessage("whisperfrost_frontiers");
        }
        if (regionLower.contains("zone4") || zoneLower.contains("devastated")) {
            return manager.getZoneMessage("devastated_lands");
        }
        if (regionLower.contains("zone0") || zoneLower.contains("ocean")) {
            return manager.getZoneMessage("ocean");
        }
        return manager.getZoneMessage("fallback");
    }

    /**
     * Creates the discovery notification text for the user prompt.
     */
    public String getDiscoveryInformation() {
        String prefix = this.discoverEntry.major() ? "Your Buddy just discovered a major new area: "
                : "Your Buddy just entered a new area: ";
        return prefix + formatZoneName(this.discoverEntry.zoneName()) + ".";
    }

    /**
     * Formats a zone name like "Emerald_Wilds" → "Emerald Wilds".
     */
    private static String formatZoneName(String zoneName) {
        return zoneName.replace("_", " ");
    }
}
