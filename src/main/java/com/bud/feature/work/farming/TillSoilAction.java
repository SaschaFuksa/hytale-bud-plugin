package com.bud.feature.work.farming;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.animations.NPCAnimationSlot;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;

/**
 * Tills the block at the position of the preceding Sensor's {@link InfoProvider} (the custom
 * {@code "Type": "WorkTarget"} Sensor exposing {@link BudComponent#getWorkTarget()}) to
 * {@code Soil_Dirt_Tilled}, provided it lies within the acting Bud's {@link WorkConfig#getFieldRadius()} of
 * its bound Workstation ({@link BudComponent#getWorkstationAnchor()}). No native Action performs this
 * transform - see docs/bud-worker-mode-plan.md, "Phase 5 — Farming-Loop Redesign".
 */
public class TillSoilAction extends ActionBase {

    private static final String TILLED_BLOCK_TYPE = "Soil_Dirt_Tilled";

    // Mirrors the removed native Block Sensor's former near-range (see docs/bud-worker-mode-plan.md,
    // "Phase 5 — Farming-Loop Redesign") - the actual "close enough to interact" gate, now enforced here
    // since the WorkTarget Sensor itself does no range-checking.
    private static final double INTERACTION_RANGE = 1.75;

    // Played directly from Java rather than a separate PlayAnimation JSON Action ahead of TillSoil in the
    // same ActionsBlocking list - see docs/bud-worker-mode-plan.md, "Animation direkt aus Java statt
    // zweiter Action".
    private static final String TILL_ANIMATION = "Interact";

    @Nonnull
    private final Vector3d target = new Vector3d();

    public TillSoilAction(@Nonnull BuilderActionTillSoil builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        if (!super.canExecute(ref, role, infoProvider, dt, store) || infoProvider == null) {
            return false;
        }
        if (!infoProvider.hasPosition()) {
            return false;
        }
        IPositionProvider positionProvider = infoProvider.getPositionProvider();
        if (positionProvider == null || !positionProvider.providePosition(target)) {
            return false;
        }
        return isWithinInteractionRange(ref, store) && isWithinFieldRadius(ref, store);
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, infoProvider, dt, store);
        playTillAnimation(ref, store);
        int x = (int) Math.floor(target.x);
        int y = (int) Math.floor(target.y);
        int z = (int) Math.floor(target.z);
        World world = store.getExternalData().getWorld();
        world.setBlock(x, y, z, TILLED_BLOCK_TYPE);
        clearOvergrowth(world, x, y, z);

        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud != null) {
            // Station picks the next candidate (after the pacing cooldown) once this clears - see
            // WorkstationFuelTickSystem.
            bud.setWorkTarget(null);
            bud.setTillCooldownSecondsRemaining(WorkConfig.getInstance().getTillIntervalSeconds());
        }
        return true;
    }

    private static void playTillAnimation(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: reached=true");
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: NPCEntity component type is null");
            return;
        }
        NPCEntity npc = store.getComponent(ref, npcType);
        if (npc == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: NPCEntity component is null");
            return;
        }

        ComponentType<EntityStore, ModelComponent> modelType = ModelComponent.getComponentType();
        ModelComponent modelComponent = modelType != null ? store.getComponent(ref, modelType) : null;
        Model model = modelComponent != null ? modelComponent.getModel() : null;
        if (model != null) {
            Set<String> availableAnimations = model.getAnimationSetMap().keySet();
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: model="
                    + model.getModelAssetId() + " hasAnimation(" + TILL_ANIMATION + ")="
                    + availableAnimations.contains(TILL_ANIMATION) + " availableAnimations=" + availableAnimations);
        } else {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: ModelComponent/Model is null "
                    + "(modelComponent=" + (modelComponent != null) + ")");
        }

        AnimationSlot mappedSlot = NPCAnimationSlot.Status.getMappedSlot();
        ComponentType<EntityStore, ActiveAnimationComponent> activeAnimType = ActiveAnimationComponent
                .getComponentType();
        ActiveAnimationComponent activeAnimationComponent = activeAnimType != null
                ? store.getComponent(ref, activeAnimType)
                : null;
        if (activeAnimationComponent == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: ActiveAnimationComponent is "
                    + "null - native playAnimation logs its own warning and returns without playing anything");
        } else if (mappedSlot != null) {
            String currentlyPlaying = activeAnimationComponent.getActiveAnimations()[mappedSlot.ordinal()];
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: slot=" + mappedSlot
                    + " animation=" + TILL_ANIMATION + " currentlyPlayingInSlot=" + currentlyPlaying
                    + " (native playAnimation no-ops for non-Action slots if this already equals the new animation)");
        }

        npc.playAnimation(ref, Objects.requireNonNull(mappedSlot), TILL_ANIMATION, store);
        LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] playTillAnimation: playAnimation call completed");
    }

    /**
     * A player's hoe swing also knocks down non-solid decor (tall grass etc.) sitting on the tilled block -
     * verified against {@code Plant_Grass_Lush.json}: such plants require {@code Type=Soil} support from
     * below, which {@code Soil_Dirt_Tilled} still carries, so the native support check alone would not drop
     * them. The block above a chosen target is already guaranteed non-solid ({@code BlockMaterial.Empty} -
     * see {@code WorkstationFuelTickSystem.hasFreeTopFace}), so anything still occupying it that isn't the
     * reserved empty/air block itself is exactly this kind of decor - clear it to match the player result.
     */
    private static void clearOvergrowth(@Nonnull World world, int x, int y, int z) {
        BlockType above = world.getBlockType(x, y + 1, z);
        if (above != null && above != BlockType.EMPTY) {
            world.setBlock(x, y + 1, z, BlockType.EMPTY_KEY);
        }
    }

    private boolean isWithinInteractionRange(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        if (transformType == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(ref, transformType);
        if (transform == null) {
            return false;
        }
        return transform.getPosition().distanceSquared(target) <= INTERACTION_RANGE * INTERACTION_RANGE;
    }

    private boolean isWithinFieldRadius(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud == null) {
            return false;
        }
        Vector3d anchor = bud.getWorkstationAnchor();
        if (anchor == null) {
            return false;
        }
        double dx = anchor.x - target.x;
        double dz = anchor.z - target.z;
        double horizontalDistanceSquared = dx * dx + dz * dz;
        double radius = WorkConfig.getInstance().getFieldRadius();
        double height = Math.abs(anchor.y - target.y);
        return horizontalDistanceSquared <= radius * radius && height <= WorkConfig.getInstance().getFieldMaxHeight();
    }

}
