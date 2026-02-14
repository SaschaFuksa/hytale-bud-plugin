package com.bud.llm.message.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.Prompt;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMWeatherManager implements ILLMChatManager {

    private final LLMWeatherMessageCreation llmCreation;

    private final String weatherId;

    public LLMWeatherManager(String weatherId) {
        this.llmCreation = new LLMWeatherMessageCreation();
        this.weatherId = weatherId;
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        if (this.weatherId == null) {
            return new DataResult<>(null, "No weather found for bud instance.");
        }
        PlayerInstance playerInstance = PlayerRegistry.getInstance()
                .getByOwner(budInstance.getOwner().getUuid());
        if (!this.hasWeatherChanged(this.weatherId, playerInstance)) {
            return new DataResult<>(null, "Weather has not changed since last check.");
        } else {
            playerInstance.setLastKnownWeather(this.weatherId);
        }
        LLMWeatherContext contextResult = LLMWeatherContext.from(this.weatherId);
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return new DataResult<>(prompt, "Weather prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        PlayerInstance playerInstance = PlayerRegistry.getInstance()
                .getByOwner(budInstance.getOwner().getUuid());
        if (!this.hasWeatherChanged(this.weatherId, playerInstance)) {
            return null;
        } else {
            LoggerUtil.getLogger().info(() -> "[BUD] Weather changed to " + this.weatherId + " for player "
                    + budInstance.getOwner().getUsername());
            playerInstance.setLastKnownWeather(this.weatherId);
        }
        return budInstance.getData().getBudMessage().getFallback("weather");
    }

    private boolean hasWeatherChanged(String currentWeatherId, PlayerInstance playerInstance) {
        String lastKnownWeatherId = playerInstance.getLastKnownWeather();
        if (lastKnownWeatherId == null) {
            return true;
        }
        return !lastKnownWeatherId.equals(currentWeatherId);
    }

}
