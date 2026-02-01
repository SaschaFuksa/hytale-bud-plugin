package com.bud.interaction;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class BudChatInteraction {

    public void sendChatMessage(World world, PlayerRef owner, String message) {
        try {
            world.execute(() -> {
                owner.sendMessage(Message.raw(message));
            });
            LoggerUtil.getLogger().finer(() -> "[BUD] Sent chat: " + message);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error sending chat: " + message);
        }
    }
    
}
