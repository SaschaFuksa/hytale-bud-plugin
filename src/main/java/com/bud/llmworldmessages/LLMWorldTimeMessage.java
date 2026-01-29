package com.bud.llmworldmessages;

import com.bud.systems.BudWorldContext;
import com.bud.systems.TimeOfDay;

public class LLMWorldTimeMessage implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context) {
        TimeOfDay time = context.timeOfDay();
        return switch (time) {
            case MORNING -> "Current time: Morning. In the morning, get ready for the next journey.";
            case DAY -> "Current time: Day. At day, go on journey.";
            case AFTERNOON -> "Current time: Afternoon. In the afternoon, take a break and enjoy the warmth.";
            case EVENING -> "Current time: Evening. In the evening, time to look for a safe place to rest.";
            case NIGHT -> "Current time: Night. In the night, monsters and undeads are lurking. It's more dangerous.";
            default -> "Time is unknown.";
        };
    }
    
}
