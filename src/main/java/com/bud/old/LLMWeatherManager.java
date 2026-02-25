package com.bud.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.llm.prompt.Prompt;

public class LLMWeatherManager {

    private final LLMWeatherMessageCreation llmCreation;

    private final String weatherId;

    public LLMWeatherManager(String weatherId) {
        this.llmCreation = new LLMWeatherMessageCreation();
        this.weatherId = weatherId;
    }

    public Prompt generatePrompt(BudInstance budInstance) {
        if (this.weatherId == null) {
            return null;
        }
        // PlayerInstance playerInstance = PlayerRegistry.getInstance()
        // .getByOwner(budInstance.getOwner().getUuid());
        // if (!this.hasWeatherChanged(this.weatherId, playerInstance)) {
        // return null;
        // } else {
        // playerInstance.setLastKnownWeather(this.weatherId);
        // }
        LLMWeatherContext contextResult = LLMWeatherContext.from(this.weatherId);
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return prompt;
    }

    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    public String getFallbackMessage(BudInstance budInstance) {
        // PlayerInstance playerInstance = PlayerRegistry.getInstance()
        // .getByOwner(budInstance.getOwner().getUuid());
        // if (!this.hasWeatherChanged(this.weatherId, playerInstance)) {
        // return null;
        // } else {
        // LoggerUtil.getLogger().info(() -> "[BUD] Weather changed to " +
        // this.weatherId + " for player "
        // + budInstance.getOwner().getUsername());
        // playerInstance.setLastKnownWeather(this.weatherId);
        // }
        // return budInstance.getData().getBudMessage().getFallback("weather");
        return null;
    }

    private boolean hasWeatherChanged(String currentWeatherId) {
        // String lastKnownWeatherId = playerInstance.getLastKnownWeather();
        // if (lastKnownWeatherId == null) {
        // return true;
        // }
        // return !lastKnownWeatherId.equals(currentWeatherId);
        return true;
    }

}
