package com.bud.core;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudState;
import static com.bud.core.types.BudState.PET_DEFENSIVE;
import com.bud.core.types.BudType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudManager {

    private static final BudManager INSTANCE = new BudManager();

    private BudManager() {
    }

    public static BudManager getInstance() {
        return INSTANCE;
    }

    public static boolean playerHasValidBud(@Nonnull PlayerBudComponent playerBudComponent, @Nonnull BudType budType) {
        if (playerBudComponent.getBudTypes().contains(budType)) {
            Optional<NPCEntity> existingBud = playerBudComponent.getCurrentBuds().stream()
                    .filter(b -> b.getNPCTypeId().equals(budType.getName()))
                    .findFirst();
            if (existingBud.isPresent()) {
                Ref<EntityStore> ref = existingBud.get().getReference();
                return isValidBud(ref);
            }
        }
        return false;
    }

    @Nonnull
    public BudState getNextState(BudState currentState) {
        return switch (currentState) {
            case PET_DEFENSIVE -> BudState.PET_PASSIVE;
            case PET_PASSIVE -> BudState.PET_SITTING;
            case PET_SITTING -> PET_DEFENSIVE;
            default -> BudState.PET_PASSIVE;
        };
    }

    public BudComponent getRandomBudComponent(PlayerBudComponent playerBudComponent) {
        if (!playerBudComponent.hasBuds()) {
            return null;
        }
        ConcurrentLinkedQueue<NPCEntity> buds = playerBudComponent.getCurrentBuds();
        int size = buds.size();
        int item = new java.util.Random().nextInt(size);
        int i = 0;
        for (NPCEntity bud : buds) {
            if (i == item)
                return getBudComponent(bud);
            i++;
        }
        return null;
    }

    @Nonnull
    public BudComponent getBudComponent(NPCEntity bud) {
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null || !isValidBud(ref)) {
            throw new IllegalStateException("Invalid Bud reference");
        }
        BudComponent component = ref.getStore().getComponent(ref, BudComponent.getComponentType());
        if (component == null) {
            throw new IllegalStateException("BudComponent not found");
        }
        return component;
    }

    @Nonnull
    public Vector3d getPlayerPositionWithOffset(PlayerRef playerRef) {
        Vector3d targetPos = getPlayerPosition(playerRef);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double offsetX = targetPos.getX() + random.nextDouble() * 6 - 3;
        double offsetY = targetPos.getY() + 0.5;
        double offsetZ = targetPos.getZ() + random.nextDouble() * 6 - 3;
        return new Vector3d(offsetX, offsetY, offsetZ);
    }

    public Vector3d getPlayerPosition(PlayerRef playerRef) {
        return playerRef.getTransform().getPosition();
    }

    public Set<BudComponent> getAllBuds() {
        World world = Universe.get().getDefaultWorld();
        if (world == null) {
            return Set.of();
        }
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ConcurrentLinkedQueue<BudComponent> allBudComponents = new ConcurrentLinkedQueue<>();
        entityStore.forEachEntityParallel(
                BudComponent.getComponentType(),
                (index, archetypeChunk, commandBuffer) -> {
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Checking entity for cleanup: " + index);
                    BudComponent budComponent = archetypeChunk.getComponent(index,
                            BudComponent.getComponentType());
                    if (budComponent == null) {
                        return;
                    }
                    allBudComponents.add(budComponent);
                });
        return Set.copyOf(allBudComponents);
    }

    public Set<PlayerBudComponent> getAllPlayers() {
        World world = Universe.get().getDefaultWorld();
        if (world == null) {
            return Set.of();
        }
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ConcurrentLinkedQueue<PlayerBudComponent> allPlayerBudComponents = new ConcurrentLinkedQueue<>();
        entityStore.forEachEntityParallel(
                PlayerBudComponent.getComponentType(),
                (index, archetypeChunk, commandBuffer) -> {
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Checking entity for cleanup: " + index);
                    PlayerBudComponent playerBudComponent = archetypeChunk.getComponent(index,
                            PlayerBudComponent.getComponentType());
                    if (playerBudComponent == null) {
                        return;
                    }
                    allPlayerBudComponents.add(playerBudComponent);
                });
        return Set.copyOf(allPlayerBudComponents);
    }

    private static boolean isValidBud(Ref<EntityStore> budRef) {
        if (budRef != null && budRef.isValid()) {
            ComponentType<EntityStore, DeathComponent> deathComponentType = DeathComponent.getComponentType();
            if (deathComponentType == null) {
                return true;
            }
            return !budRef.getStore().getArchetype(budRef).contains(deathComponentType);
        }
        return false;
    }

}