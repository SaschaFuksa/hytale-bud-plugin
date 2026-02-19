package com.bud;

import com.bud.config.ReactionConfig;
import com.bud.llm.message.state.LLMStateManager;
import com.bud.npc.BudRegistry;
import com.bud.npc.buds.IBudData;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.tracker.StateTracker;
import com.bud.reaction.tracker.WeatherTracker;
import com.bud.reaction.tracker.WorldTracker;
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

    private static final PlayerRegistry playerRegistry = PlayerRegistry.getInstance();

    private static final ReactionConfig config = ReactionConfig.getInstance();

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

        boolean isFirstBud = budRegistry.getAllOwners().isEmpty();
        budRegistry.register(owner, bud, budNPCData,
                mainStateName);

        // Start polling when at least one Bud is tracked
        checkAndStartTrackers(isFirstBud);
        return new SuccessResult("Bud registered for tracking for player " + owner.getUuid());
    }

    public IResult registerPlayer(PlayerRef owner) {
        if (playerRegistry.getByOwner(owner.getUuid()) != null) {
            return new SuccessResult("Player already registered for tracking for player " + owner.getUuid());
        }
        Weather weather = WorldInformationUtil.getCurrentWeather(owner);

        boolean isFirstPlayer = playerRegistry.getAllOwners().isEmpty();
        playerRegistry.register(owner, weather != null ? weather.getId() : null);

        // Start polling when at least one player is tracked
        checkAndStartTrackers(isFirstPlayer);
        return new SuccessResult("Player registered for tracking for player " + owner.getUuid());
    }

    private void checkAndStartTrackers(boolean isFirst) {
        if (isFirst) {
            registerTracker();
        }
    }

    private void registerTracker() {
        StateTracker.getInstance().startPolling();
        if (RegistryManager.config.isEnableWorldReactions()) {
            WorldTracker.getInstance().startPolling();
        }
        if (RegistryManager.config.isEnableWeatherReactions()) {
            WeatherTracker.getInstance().startPolling();
        }
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
            stopAllTrackers();
        }
        return new SuccessResult("Stopped tracking for bud " + bud.getUuid());
    }

    private IResult unregisterPlayer(PlayerRef player) {
        playerRegistry.unregister(player.getUuid());
        if (playerRegistry.getAllOwners().isEmpty()) {
            stopAllTrackers();
        }
        return new SuccessResult("Stopped tracking for player " + player.getUuid());
    }

    private void stopAllTrackers() {
        StateTracker.getInstance().stopPolling();
        WorldTracker.getInstance().stopPolling();
        WeatherTracker.getInstance().stopPolling();
    }

}
