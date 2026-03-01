package com.bud.feature.bud.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.Inventory;
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
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private boolean withInventory = false;
    private int inventoryRows = 3;
    private int inventoryColumns = 9;
    private final List<WeaponConfig> weapons = new ArrayList<>();
    private final List<ArmorConfig> armors = new ArrayList<>();

    private Ref<EntityStore> spawnedNpcRef;

    private BudSpawner(Store<EntityStore> store, String npcType, Vector3d position) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.npcType = Objects.requireNonNull(npcType, "npcType cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
    }

    public static BudSpawner create(Store<EntityStore> store, String npcType, Vector3d position) {
        LoggerUtil.getLogger().fine(() -> "[BUD] ========================================");
        LoggerUtil.getLogger().fine(() -> "[BUD] Attempting to spawn custom NPC Type : " + npcType);
        LoggerUtil.getLogger().fine(() -> "[BUD] Position: " + position);
        LoggerUtil.getLogger().fine(() -> "[BUD] ========================================");
        return new BudSpawner(store, npcType, position);
    }

    public BudSpawner withRotation(@Nonnull Vector3f rotation) {
        this.rotation = rotation;
        return this;
    }

    public BudSpawner withInventory() {
        this.withInventory = true;
        return this;
    }

    public BudSpawner withInventory(int rows, int columns) {
        this.withInventory = true;
        this.inventoryRows = rows;
        this.inventoryColumns = columns;
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

            spawnedNpcRef = result.first();

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
        ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
        if (componentType == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[NPCSpawner] Cannot configure inventory: NPCEntity component type is null");
            return;
        }
        if (spawnedNpcRef == null) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure inventory: NPC reference is null");
            return;
        }
        NPCEntity npcComponent = store.getComponent(spawnedNpcRef, componentType);
        if (npcComponent == null) {
            LoggerUtil.getLogger().severe(() -> "[NPCSpawner] Cannot configure inventory: NPC component not found");
            return;
        }

        npcComponent.setInventorySize(inventoryRows, inventoryColumns, 0);
        Inventory inventory = npcComponent.getInventory();

        for (WeaponConfig weapon : weapons) {
            ItemStack itemStack = new ItemStack(weapon.itemId, weapon.quantity);
            inventory.getHotbar().addItemStackToSlot(weapon.slot, itemStack);
        }

        if (!weapons.isEmpty()) {
            inventory.setActiveHotbarSlot((byte) weapons.get(0).slot);
        }

        for (ArmorConfig armor : armors) {
            ItemContainer armorContainer = inventory.getArmor();
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
