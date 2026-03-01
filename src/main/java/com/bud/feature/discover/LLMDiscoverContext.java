package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.world.env.ZoneMessage;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMDiscoverContext(DiscoverEntry discoverEntry) implements IPromptContext {

    @Nonnull
    public static LLMDiscoverContext from(DiscoverEntry entry) {
        return new LLMDiscoverContext(entry);
    }

    public ZoneMessage getZoneInfo(LLMPromptManager manager) {
        String regionLower = this.discoverEntry.regionName().toLowerCase();
        String zoneLower = this.discoverEntry.zoneName().toLowerCase();

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

    public String getDiscoveryInformation() {
        String prefix = this.discoverEntry.major() ? "Your Buddy just discovered a major new area: "
                : "Your Buddy just entered a new area: ";
        return prefix + formatZoneName(this.discoverEntry.zoneName()) + ".";
    }

    private static String formatZoneName(String zoneName) {
        return zoneName.replace("_", " ");
    }

    @Override
    public BudComponent getBudComponent() {
        return discoverEntry.budComponent();
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(discoverEntry.budComponent().getBudType());
    }
}
