package com.bud.player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerRegistry {

    private static final PlayerRegistry INSTANCE = new PlayerRegistry();

    private final Map<UUID, PlayerInstance> byOwner = new ConcurrentHashMap<>();

    private PlayerRegistry() {
    }

    public static PlayerRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(PlayerRef playerRef, String lastKnownWeather) {
        PlayerInstance instance = new PlayerInstance(playerRef, lastKnownWeather);
        byOwner.putIfAbsent(playerRef.getUuid(), instance);
    }

    public synchronized void unregister(UUID playerId) {
        byOwner.remove(playerId);
    }

    public PlayerInstance getByOwner(UUID ownerId) {
        return byOwner.getOrDefault(ownerId, null);
    }

    public Set<UUID> getAllOwners() {
        return byOwner.keySet();
    }

}
