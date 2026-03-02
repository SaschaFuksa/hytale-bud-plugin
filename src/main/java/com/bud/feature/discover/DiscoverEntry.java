package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.world.env.ZoneMessage;

public record DiscoverEntry(@Nonnull String zoneName, @Nonnull String regionName, boolean major,
        @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    public ZoneMessage getZoneInfo(LLMPromptManager manager) {
        String regionLower = this.regionName.toLowerCase();
        String zoneLower = this.zoneName.toLowerCase();

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
        String prefix = this.major ? "Your Buddy just discovered a major new area: "
                : "Your Buddy just entered a new area: ";
        return prefix + formatZoneName(this.zoneName) + ".";
    }

    private static String formatZoneName(String zoneName) {
        return zoneName.replace("_", " ");
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return zoneName;
    }
}
