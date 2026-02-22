package com.bud.llm.orchestrator;

/**
 * Defines the message channels for grouping related events.
 * Each channel has its own cooldown and priority queue.
 */
public enum MessageChannel {

    /**
     * Ambient / environmental events:
     * Zone discoveries, weather changes, world ambient messages.
     */
    AMBIENT,

    /**
     * Player activity events:
     * Crafting, bench usage, item pickups, block interactions.
     */
    ACTIVITY,

    /**
     * Combat events:
     * Damage dealt/taken, kills, etc.
     */
    COMBAT
}
