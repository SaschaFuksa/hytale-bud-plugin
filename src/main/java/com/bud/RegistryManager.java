package com.bud;

import com.bud.llm.message.state.LLMStateManager;
import com.bud.npc.BudRegistry;
import com.bud.npc.BudStateTracker;
import com.bud.npc.buds.IBudData;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.WeatherTracker;
import com.bud.reaction.world.WorldInformationUtil;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

public class RegistryManager {

    private static final RegistryManager INSTANCE = new RegistryManager();

    private static final BudRegistry budRegistry = BudRegistry.getInstance();

    private static final BudStateTracker budStateTracker = BudStateTracker.getInstance();

    private static final PlayerRegistry playerRegistry = PlayerRegistry.getInstance();

    private static final WeatherTracker weatherTracker = WeatherTracker.getInstance();

    private static final BudConfig config = BudConfig.getInstance();

    private RegistryManager() {
    }

    public static RegistryManager getInstance() {
        return INSTANCE;
    }

    public IResult registerBud(PlayerRef owner, NPCEntity bud, IBudData budNPCData) {
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null) {
            return new ErrorResult("Bud NPC has no valid reference");
        }
        Role role = bud.getRole();
        if (role == null) {
            return new ErrorResult("Bud NPC has no valid Role");
        }
        String mainStateName = LLMStateManager.getMainStateName(role.getStateSupport().getStateName());
        budRegistry.register(owner, bud, budNPCData,
                mainStateName);

        // Start polling when at least one Bud is tracked
        budStateTracker.startPolling();
        return new SuccessResult("Bud registered for tracking for player " + owner.getUuid());
    }

    public IResult registerPlayer(PlayerRef owner) {
        if (config.isEnableWeatherReactions()) {
            if (playerRegistry.getByOwner(owner.getUuid()) != null) {
                return new SuccessResult("Player already registered for tracking for player " + owner.getUuid());
            }
            Weather weather = WorldInformationUtil.getCurrentWeather(owner);
            playerRegistry.register(owner.getUuid(), weather != null ? weather.getId() : null);

            weatherTracker.startPolling();
            return new SuccessResult("Player registered for tracking for player " + owner.getUuid());
        }
        return new SuccessResult("Weather reactions are disabled; player not registered.");
    }

    public IResult unregister(NPCEntity bud, PlayerRef player) {
        boolean isSuccess = true;
        try {
            this.unregisterBud(bud);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error unregistering Bud: " + e.getMessage());
            isSuccess = false;
        }
        try {
            this.unregisterPlayer(player);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error unregistering player: " + e.getMessage());
            isSuccess = false;
        }
        if (!isSuccess) {
            return new ErrorResult("Error unregistering Bud or player. Check logs for details.");
        } else {
            return new SuccessResult("Successfully unregistered Bud and player.");
        }
    }

    private IResult unregisterBud(NPCEntity bud) {
        budRegistry.unregister(bud);
        if (budRegistry.getAllRefs().isEmpty()) {
            budStateTracker.stopPolling();
        }
        return new SuccessResult("Stopped tracking for bud " + bud.getUuid());
    }

    private IResult unregisterPlayer(PlayerRef player) {
        playerRegistry.unregister(player.getUuid());
        if (playerRegistry.getAllOwners().isEmpty()) {
            weatherTracker.stopPolling();
        }
        return new SuccessResult("Stopped tracking for player " + player.getUuid());
    }

}
