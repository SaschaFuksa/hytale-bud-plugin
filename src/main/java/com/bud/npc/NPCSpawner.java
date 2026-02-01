package com.bud.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

import it.unimi.dsi.fastutil.Pair;

/**
 * Builder pattern for spawning and configuring NPCs with fluent API.
 * 
 * Example usage:
 * 
 * <pre>
 * NPCSpawner.create(store, "Feran_Civilian", position)
 *         .withInventory()
 *         .addWeapon("Weapon_Shortbow_Iron")
 *         .addArmor("Armor_Thorium_Head")
 *         .spawn();
 * </pre>
 */
public class NPCSpawner {

    // Required parameters
    private final Store<EntityStore> store;
    private final String npcType;
    private final Vector3d position;

    // Optional parameters with defaults
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private boolean withInventory = false;
    private int inventoryRows = 3;
    private int inventoryColumns = 9;
    private final List<WeaponConfig> weapons = new ArrayList<>();
    private final List<ArmorConfig> armors = new ArrayList<>();

    // Result
    private Ref<EntityStore> spawnedNpcRef;

    /**
     * Private constructor - use create() factory method
     */
    private NPCSpawner(Store<EntityStore> store, String npcType, Vector3d position) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.npcType = Objects.requireNonNull(npcType, "npcType cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
    }

    /**
     * Creates a new NPCSpawner builder.
     * 
     * @param store    The entity store
     * @param npcType  The NPC type ID (e.g., "Feran_Civilian")
     * @param position The spawn position
     * @return A new NPCSpawner builder
     */
    public static NPCSpawner create(Store<EntityStore> store, String npcType, Vector3d position) {
        System.out.println("[BUD] ========================================");
        System.out.println("[BUD] Attempting to spawn custom NPC Type : " + npcType);
        System.out.println("[BUD] Position: " + position);
        System.out.println("[BUD] ========================================");
        return new NPCSpawner(store, npcType, position);
    }

    /**
     * Sets the spawn rotation.
     * 
     * @param rotation The rotation vector
     * @return This builder for chaining
     */
    public NPCSpawner withRotation(Vector3f rotation) {
        this.rotation = rotation;
        return this;
    }

    /**
     * Enables inventory for this NPC with default size (3x9).
     * 
     * @return This builder for chaining
     */
    public NPCSpawner withInventory() {
        this.withInventory = true;
        return this;
    }

    /**
     * Enables inventory with custom size.
     * 
     * @param rows    Number of rows (typically 3)
     * @param columns Number of columns (typically 9)
     * @return This builder for chaining
     */
    public NPCSpawner withInventory(int rows, int columns) {
        this.withInventory = true;
        this.inventoryRows = rows;
        this.inventoryColumns = columns;
        return this;
    }

    /**
     * Adds a weapon to the NPC's hotbar.
     * 
     * @param weaponType The weapon item ID (e.g., "Weapon_Shortbow_Iron")
     * @return This builder for chaining
     */
    public NPCSpawner addWeapon(String weaponType) {
        return addWeapon(weaponType, 1);
    }

    /**
     * Adds a weapon with quantity to the NPC's hotbar.
     * 
     * @param weaponType The weapon item ID
     * @param quantity   The quantity
     * @return This builder for chaining
     */
    public NPCSpawner addWeapon(String weaponType, int quantity) {
        return addWeapon(weaponType, quantity, (short) 0);
    }

    /**
     * Adds a weapon to a specific hotbar slot.
     * 
     * @param weaponType The weapon item ID
     * @param quantity   The quantity
     * @param slot       The hotbar slot (0-8)
     * @return This builder for chaining
     */
    public NPCSpawner addWeapon(String weaponType, int quantity, short slot) {
        this.weapons.add(new WeaponConfig(weaponType, quantity, slot));
        return this;
    }

    /**
     * Adds armor to the NPC.
     * 
     * @param armorType The armor item ID (e.g., "Armor_Thorium_Head")
     * @return This builder for chaining
     */
    public NPCSpawner addArmor(String armorType) {
        this.armors.add(new ArmorConfig(armorType));
        return this;
    }

    /**
     * Spawns the NPC with all configured settings.
     * 
     * @return The spawned NPC reference, or null if spawn failed
     */
    public Pair<Ref<EntityStore>, INonPlayerCharacter> spawn() {
        try {
            // Spawn the NPC
            Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get().spawnNPC(store, npcType, "Player",
                    position, rotation);

            if (result == null) {
                System.out.println("[NPCSpawner] Failed to spawn NPC: " + npcType);
                return null;
            }

            spawnedNpcRef = result.first();

            // Configure inventory if requested
            if (withInventory) {
                configureInventory();
            }

            System.out.println("[NPCSpawner] Successfully spawned " + npcType + " at " + position);
            return result;

        } catch (Exception e) {
            System.out.println("[NPCSpawner] Error spawning NPC: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Configures the inventory for the spawned NPC.
     */
    private void configureInventory() {
        NPCEntity npcComponent = store.getComponent(spawnedNpcRef, NPCEntity.getComponentType());
        if (npcComponent == null) {
            System.out.println("[NPCSpawner] Cannot configure inventory: NPC component not found");
            return;
        }

        // Initialize inventory size
        npcComponent.setInventorySize(inventoryRows, inventoryColumns, 0);
        Inventory inventory = npcComponent.getInventory();

        // Add weapons to hotbar
        for (WeaponConfig weapon : weapons) {
            ItemStack itemStack = new ItemStack(weapon.itemId, weapon.quantity);
            inventory.getHotbar().addItemStackToSlot(weapon.slot, itemStack);
        }

        // Set first weapon as active if any weapons were added
        if (!weapons.isEmpty()) {
            inventory.setActiveHotbarSlot((byte) weapons.get(0).slot);
        }

        // Add armor
        for (ArmorConfig armor : armors) {
            InventoryHelper.useArmor(inventory.getArmor(), armor.itemId);
        }

        System.out.println("[NPCSpawner] Configured inventory: " + weapons.size() + " weapons, " + armors.size()
                + " armor pieces");
    }

    /**
     * Gets the reference to the spawned NPC (available after spawn() is called).
     * 
     * @return The NPC reference, or null if not yet spawned
     */
    public Ref<EntityStore> getSpawnedNpc() {
        return spawnedNpcRef;
    }

    // ========== Configuration Classes ==========

    private static class WeaponConfig {
        final String itemId;
        final int quantity;
        final short slot;

        WeaponConfig(String itemId, int quantity, short slot) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.slot = slot;
        }
    }

    private static class ArmorConfig {
        final String itemId;

        ArmorConfig(String itemId) {
            this.itemId = itemId;
        }
    }
}
