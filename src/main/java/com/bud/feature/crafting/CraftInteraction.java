package com.bud.feature.crafting;

/**
 * Represents the type of crafting interaction.
 */
public enum CraftInteraction {
    /** Player crafted an item using a crafting recipe (CraftRecipeEvent). */
    CRAFTED,
    /** Player used a processing bench (e.g. cooking, alchemy, furnace). */
    USED
}
