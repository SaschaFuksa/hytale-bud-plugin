package com.bud.feature.discover;

import com.bud.core.components.BudComponent;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.feature.profile.IBudProfile;
import com.bud.feature.world.env.ZoneMessage;

/**
 * Context for discover zone LLM prompts.
 * Resolves zone/region names to existing zone descriptions.
 */
public record LLMDiscoverContext(DiscoverEntry discoverEntry) implements IPromptContext {

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

    @Override
    public BudComponent getBudComponent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudComponent'");
    }

    @Override
    public IBudProfile getBudProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudProfile'");
    }
}
