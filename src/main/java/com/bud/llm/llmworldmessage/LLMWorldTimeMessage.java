package com.bud.llm.llmworldmessage;

import com.bud.system.BudWorldContext;
import com.bud.system.TimeOfDay;

public class LLMWorldTimeMessage implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        TimeOfDay time = context.timeOfDay();
        return switch (time) {
            case MORNING -> "Current time: Morning. In the morning, get ready for the journey ahead.";
            case DAY -> "Current time: Day. During the day, continue your journey.";
            case AFTERNOON -> "Current time: Afternoon. In the afternoon, take a break and enjoy the warmth.";
            case EVENING -> "Current time: Evening. In the evening, it's time to look for a safe place to rest.";
            case NIGHT -> "Current time: Night. At night, monsters and undead are lurking. It is more dangerous.";
            default -> "Time is unknown.";
        };
    }

}
