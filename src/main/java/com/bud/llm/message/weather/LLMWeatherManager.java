package com.bud.llm.message.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.Prompt;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.WorldInformationUtil;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.world.World;

public class LLMWeatherManager implements ILLMChatManager {

    private final LLMWeatherMessageCreation llmCreation;

    public LLMWeatherManager() {
        this.llmCreation = new LLMWeatherMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        World world = WorldInformationUtil.resolveWorld(budInstance.getOwner());
        if (world == null) {
            return new DataResult<>(null, "No world found for bud instance.");
        }

        try {
            // We must fetch weather data on the world thread
            return CompletableFuture.supplyAsync(() -> {
                Weather weather = WorldInformationUtil.getCurrentWeather(budInstance.getOwner());
                if (weather == null) {
                    return new DataResult<Prompt>(null, "No weather found for bud instance.");
                }
                PlayerInstance playerInstance = PlayerRegistry.getInstance()
                        .getByOwner(budInstance.getOwner().getUuid());
                if (!this.hasWeatherChanged(weather, playerInstance)) {
                    return new DataResult<Prompt>(null, "Weather has not changed since last check.");
                } else {
                    playerInstance.setLastKnownWeather(weather.getId());
                }
                LLMWeatherContext contextResult = LLMWeatherContext.from(weather.getId());
                Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
                return new DataResult<Prompt>(prompt, "Weather prompt generation.");
            }, world::execute).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error generating prompt in world thread: " + e.getMessage());
            return new DataResult<>(null, "Thread error: " + e.getMessage());
        }
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
        World world = WorldInformationUtil.resolveWorld(budInstance.getOwner());
        if (world == null) {
            return null;
        }

        try {
            return CompletableFuture.supplyAsync(() -> {
                Weather weather = WorldInformationUtil.getCurrentWeather(budInstance.getOwner());
                if (weather == null) {
                    return null;
                }
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
            }, world::execute).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error getting fallback in world thread: " + e.getMessage());
            return null;
        }
    }

    private boolean hasWeatherChanged(Weather currentWeather, PlayerInstance playerInstance) {
        String lastKnownWeatherId = playerInstance.getLastKnownWeather();
        if (lastKnownWeatherId == null) {
            return true;
        }
        return !lastKnownWeatherId.equals(currentWeather.getId());
    }

}
