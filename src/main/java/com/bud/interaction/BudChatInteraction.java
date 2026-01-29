package com.bud.interaction;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class BudChatInteraction {

    public void sendChatMessage(World world, PlayerRef owner, String message) {
        try {
            world.execute(() -> {
                owner.sendMessage(Message.raw(message));
            });
            System.out.println("[BUD] Sent chat: " + message);
        } catch (Exception e) {
            System.out.println("[BUD] Failed to send chat: " + e.getMessage());
        }
    }
    
}
