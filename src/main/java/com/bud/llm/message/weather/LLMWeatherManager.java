package com.bud.llm.message.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.bud.util.WorldInformationUtil;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;

public class LLMWeatherManager implements ILLMChatManager {

    private final LLMWeatherMessageCreation llmCreation;

    public LLMWeatherManager() {
        this.llmCreation = new LLMWeatherMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        Weather weather = WorldInformationUtil.getCurrentWeather(budInstance);
        if (weather == null) {
            return new DataResult<>(null, "No weather found for bud instance.");
        }
        PlayerInstance playerInstance = PlayerRegistry.getInstance().getByOwner(budInstance.getOwner().getUuid());
        if (!this.hasWeatherChanged(weather, playerInstance)) {
            return new DataResult<>(null, "Weather has not changed since last check.");
        } else {
            playerInstance.setLastKnownWeather(weather.getId());
        }
        LLMWeatherContext contextResult = LLMWeatherContext.from(weather.getId());
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance.getData().getBudMessage());
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
        return budInstance.getData().getBudMessage().getFallback("weather");
    }

    private boolean hasWeatherChanged(Weather currentWeather, PlayerInstance playerInstance) {
        String lastKnownWeatherId = playerInstance.getLastKnownWeather();
        return !lastKnownWeatherId.equals(currentWeather.getId());
    }

}
