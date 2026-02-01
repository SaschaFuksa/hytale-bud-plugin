package com.bud.llm.llmworldmessage;

import com.bud.system.BudWorldContext;

public class LLMWorldZoneMessage implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String currentZoneName = context.currentZone().name().toLowerCase();
        if (currentZoneName.contains("1") || currentZoneName.contains("emerald") || currentZoneName.contains("grove")) {
            return "Current zone: Emerald Grove: This zone is safe and calm. It's moderate and sometimes rainy. Plains, forest, swamps and caves are present.";
        } else if (currentZoneName.contains("2") || currentZoneName.contains("howling")
                || currentZoneName.contains("sands")) {
            return "Current zone: Howling Sands: This zone is adventurous with toxic enemies. It's hot and sandstorms can appear. Desert, badlands, steppes, oasis and canyons are present.";
        } else if (currentZoneName.contains("3") || currentZoneName.contains("whisperfrost")
                || currentZoneName.contains("frontiers")) {
            return "Current zone: Whisperfrost Frontiers: This zone is harsh with frost and magic enemies. It's cold and snow storms can appear. Tundra, boreal forest, everfrost, glacier, mountains and icy caves are present.";
        } else if (currentZoneName.contains("4") || currentZoneName.contains("devastated")) {
            return "Current zone: Devastated Lands: This zone is dangerous and volcanic. It's hot with a broken surface. Volcanoes, ashlands, everburning woods and mushroom forests are present. Under ground is a tropical jungle with dinosaurs.";
        } else if (currentZoneName.contains("ocean")) {
            return "Current zone: Ocean: This zone is vast and watery. It's moderate and separates continents. Deep ocean, ocean shelf and volcanic islands are present.";
        }
        return "The current zone is unknown.";
    }

}