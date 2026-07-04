package com.bud.feature.bud.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

import it.unimi.dsi.fastutil.Pair;

public class BudSpawner {

    @Nonnull
    private final Store<EntityStore> store;
    @Nonnull
    private final String npcType;
    @Nonnull
    private final Vector3d position;

    @Nonnull
    private Rotation3f rotation = new Rotation3f(0, 0, 0);
    private boolean withInventory = false;
    private final List<WeaponConfig> weapons = new ArrayList<>();
    private final List<ArmorConfig> armors = new ArrayList<>();

    @Nullable
    private Ref<EntityStore> spawnedNpcRef;

    private BudSpawner(@Nonnull Store<EntityStore> store, @Nonnull String npcType, @Nonnull Vector3d position) {
        this.store = store;
        this.npcType = npcType;
        this.position = position;
    }

    public static BudSpawner create(@Nonnull Store<EntityStore> store, @Nonnull String npcType,
            @Nonnull Vector3d position) {
        LoggerUtil.getLogger().fine(() -> "[BUD] ========================================");
        LoggerUtil.getLogger().fine(() -> "[BUD] Attempting to spawn custom NPC Type : " + npcType);
        LoggerUtil.getLogger().fine(() -> "[BUD] Position: " + position);
        LoggerUtil.getLogger().fine(() -> "[BUD] ========================================");
        return new BudSpawner(store, npcType, position);
    }

    public BudSpawner withRotation(@Nonnull Vector3f rotation) {
        this.rotation = new Rotation3f(rotation.x, rotation.y, rotation.z);
        return this;
    }

    public BudSpawner withInventory() {
        this.withInventory = true;
        return this;
    }

    public BudSpawner addWeapon(@Nonnull String weaponType) {
        return addWeapon(weaponType, 1);
    }

    public BudSpawner addWeapon(@Nonnull String weaponType, int quantity) {
        return addWeapon(weaponType, quantity, (short) 0);
    }

    public BudSpawner addWeapon(@Nonnull String weaponType, int quantity, short slot) {
        this.weapons.add(new WeaponConfig(weaponType, quantity, slot));
        return this;
    }

    public BudSpawner addArmor(@Nonnull String armorType) {
        this.armors.add(new ArmorConfig(armorType));
        return this;
    }

    public Pair<Ref<EntityStore>, INonPlayerCharacter> spawn() {
        try {
            Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get().spawnNPC(store, npcType, "Player",
                    position, rotation);

            if (result == null) {
                LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Failed to spawn NPC: " + npcType);
                return null;
            }

            spawnedNpcRef = Objects.requireNonNull(result.first(), "Spawned NPC reference was null");

            if (withInventory) {
                configureInventory();
            }

            LoggerUtil.getLogger().fine(() -> "[NPCSpawner] Successfully spawned " + npcType + " at " + position);
            return result;

        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Error spawning NPC: " + e.getMessage());
            return null;
        }
    }

    private void configureInventory() {
        ComponentType<EntityStore, NPCEntity> componentType = Objects.requireNonNull(
                NPCEntity.getComponentType(), "NPCEntity component type is null");
        Ref<EntityStore> npcRef = Objects.requireNonNull(this.spawnedNpcRef, "NPC reference is null");
        NPCEntity npcComponent = store.getComponent(npcRef, componentType);
        if (npcComponent == null) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure inventory: NPC component not found");
            return;
        }
        ComponentAccessor<EntityStore> accessor = store;
        ComponentType<EntityStore, InventoryComponent.Hotbar> hotbarType = Objects.requireNonNull(
                InventoryComponent.Hotbar.getComponentType(), "Hotbar component type is null");
        InventoryComponent.Hotbar hotbar = accessor.getComponent(npcRef, hotbarType);
        if (hotbar == null) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure inventory: Hotbar component not found");
            return;
        }
        ItemContainer inventory = hotbar.getInventory();

        for (WeaponConfig weapon : weapons) {
            ItemStack itemStack = new ItemStack(weapon.itemId, weapon.quantity);
            inventory.addItemStackToSlot(weapon.slot, itemStack);
        }

        if (!weapons.isEmpty()) {
            hotbar.setActiveSlot((byte) weapons.get(0).slot, npcRef, accessor);
        }

        ComponentType<EntityStore, InventoryComponent.Armor> armorType = Objects.requireNonNull(
                InventoryComponent.Armor.getComponentType(), "Armor component type is null");
        InventoryComponent.Armor armorComponent = accessor.getComponent(npcRef, armorType);
        if (armorComponent == null) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure armor: Armor component not found");
            return;
        }
        for (ArmorConfig armor : armors) {
            ItemContainer armorContainer = armorComponent.getInventory();
            if (armorContainer == null) {
                LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure armor: Armor container not found");
                continue;
            }
            InventoryHelper.useArmor(armorContainer, armor.itemId);
        }

        LoggerUtil.getLogger()
                .fine(() -> "[NPCSpawner] Configured inventory: " + weapons.size() + " weapons, " + armors.size()
                        + " armor pieces");
    }

    private static class WeaponConfig {

        @Nonnull
        final String itemId;
        final int quantity;
        final short slot;

        WeaponConfig(@Nonnull String itemId, int quantity, short slot) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.slot = slot;
        }
    }

    private static class ArmorConfig {
        @Nonnull
        final String itemId;

        ArmorConfig(@Nonnull String itemId) {
            this.itemId = itemId;
        }
    }
}
