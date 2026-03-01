package com.bud.old;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudState;
import static com.bud.core.types.BudState.PET_DEFENSIVE;
import com.bud.core.types.BudType;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
                if (ref != null && ref.isValid()) {
                    ComponentType<EntityStore, DeathComponent> deathComponentType = DeathComponent.getComponentType();
                    if (deathComponentType == null) {
                        return true;
                    }
                    return !ref.getStore().getArchetype(ref).contains(deathComponentType);
                }
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

    public NPCEntity getRandomBud(UUID ownerId) {
        // TODO
        Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);

        if (buds.isEmpty()) {
            return null;
        }

        int size = buds.size();
        int item = new java.util.Random().nextInt(size);
        int i = 0;
        for (BudInstance instance : buds) {
            if (i == item)
                return instance.getEntity();
            i++;
        }
        return null;
    }

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

}