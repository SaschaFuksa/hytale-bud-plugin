package com.bud.player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    private static final PlayerRegistry INSTANCE = new PlayerRegistry();

    private final Map<UUID, PlayerInstance> byOwner = new ConcurrentHashMap<>();

    private PlayerRegistry() {
    }

    public static PlayerRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(UUID playerId, String lastKnownWeather) {
        PlayerInstance instance = new PlayerInstance(lastKnownWeather);
        byOwner.putIfAbsent(playerId, instance);
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
