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
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;

public class LLMWeatherManager implements ILLMChatManager {

    private final LLMWeatherMessageCreation llmCreation;

    private final Weather weather;

    public LLMWeatherManager(Weather weather) {
        this.llmCreation = new LLMWeatherMessageCreation();
        this.weather = weather;
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        if (this.weather == null) {
            return new DataResult<>(null, "No weather found for bud instance.");
        }
        PlayerInstance playerInstance = PlayerRegistry.getInstance()
                .getByOwner(budInstance.getOwner().getUuid());
        if (!this.hasWeatherChanged(this.weather, playerInstance)) {
            return new DataResult<>(null, "Weather has not changed since last check.");
        } else {
            playerInstance.setLastKnownWeather(this.weather.getId());
        }
        LLMWeatherContext contextResult = LLMWeatherContext.from(this.weather.getId());
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
        if (!this.hasWeatherChanged(weather, playerInstance)) {
            return null;
        } else {
            LoggerUtil.getLogger().info(() -> "[BUD] Weather changed to " + weather.getId() + " for player "
                    + budInstance.getOwner().getUsername());
            playerInstance.setLastKnownWeather(weather.getId());
        }
        return budInstance.getData().getBudMessage().getFallback("weather");
    }

    private boolean hasWeatherChanged(Weather currentWeather, PlayerInstance playerInstance) {
        String lastKnownWeatherId = playerInstance.getLastKnownWeather();
        if (lastKnownWeatherId == null) {
            return true;
        }
        return !lastKnownWeatherId.equals(currentWeather.getId());
    }

}
