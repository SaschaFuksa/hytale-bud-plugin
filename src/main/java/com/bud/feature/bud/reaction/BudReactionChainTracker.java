package com.bud.feature.bud.reaction;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ConversationConfig;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Lets a Bud2Bud reaction chain into a named reply when the generated message mentions
 * another active Bud by name, up to {@link ConversationConfig#getBudReactionChainMaxReplies()}
 * replies. Any of the "original" trigger kinds resets the chain for that player.
 */
public class BudReactionChainTracker {

    @Nonnull
    private static final BudReactionChainTracker INSTANCE = new BudReactionChainTracker();

    private final Map<String, Integer> chainDepthByPlayer = new ConcurrentHashMap<>();

    private BudReactionChainTracker() {
    }

    @Nonnull
    public static BudReactionChainTracker getInstance() {
        return Objects.requireNonNull(INSTANCE);
    }

    public void onReactionDispatched(@Nonnull BudReactionEntry entry, @Nonnull String message) {
        BudComponent speaker = entry.budComponent();
        Ref<EntityStore> speakerRef = speaker.getBud().getReference();
        if (speakerRef == null) {
            return;
        }
        Store<EntityStore> store = speakerRef.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            PlayerRef playerRef = speaker.getPlayerRef();
            String playerKey = playerRef.getUsername().toLowerCase();

            int depth;
            if (entry.kind() == BudReactionKind.NAMED_REPLY) {
                depth = this.chainDepthByPlayer.getOrDefault(playerKey, 0);
            } else {
                depth = 0;
                this.chainDepthByPlayer.put(playerKey, 0);
            }

            if (depth >= ConversationConfig.getInstance().getBudReactionChainMaxReplies()) {
                return;
            }

            Ref<EntityStore> playerEntityRef = playerRef.getReference();
            if (playerEntityRef == null) {
                return;
            }
            PlayerBudComponent playerBudComponent = store.getComponent(playerEntityRef,
                    PlayerBudComponent.getComponentType());
            if (playerBudComponent == null) {
                return;
            }

            BudComponent mentionedBud = BudManager.getInstance()
                    .findBudByNameMention(playerBudComponent, message, speaker);
            if (mentionedBud == null) {
                return;
            }

            this.chainDepthByPlayer.put(playerKey, depth + 1);

            String speakerName = BudProfileMapper.getInstance().getProfileForBudType(speaker.getBudType())
                    .getNPCDisplayName();
            String situationInfo = speakerName + " just said to you: \"" + message
                    + "\". Reply to them directly, in character.";
            BudReactionEntry replyEntry = new BudReactionEntry(mentionedBud, BudReactionKind.NAMED_REPLY,
                    situationInfo);
            long now = System.currentTimeMillis();
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.SOCIAL,
                    replyEntry,
                    replyEntry.getEntryName() + ":" + now,
                    playerRef.getUsername(),
                    new LLMInteractionEntry(LLMBudReactionMessageCreation.getInstance(), replyEntry),
                    now));
            LoggerUtil.getLogger().fine(() -> "[BUD] Chained named reply (" + (depth + 1) + "/"
                    + ConversationConfig.getInstance().getBudReactionChainMaxReplies() + ") for player "
                    + playerRef.getUsername());
        });
    }

    public void clearPlayer(@Nonnull String playerName) {
        this.chainDepthByPlayer.remove(playerName.toLowerCase());
    }
}
