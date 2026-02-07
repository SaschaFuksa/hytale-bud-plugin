package com.bud.llm.message.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.block.RecentBlockCache;
import com.bud.block.RecentBlockCache.BlockEntry;
import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;

public class LLMBlockManager implements ILLMChatManager {

    private static final LLMBlockManager INSTANCE = new LLMBlockManager();
    public static final String NO_BLOCK_STRING = "No recent block interactions.";

    private final LLMBlockMessageCreation llmCreation;

    private LLMBlockManager() {
        this.llmCreation = new LLMBlockMessageCreation();
    }

    public static LLMBlockManager getInstance() {
        return INSTANCE;
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        BlockEntry latestEntry = RecentBlockCache.pollHistory(budInstance.getOwner().getUuid());
        if (latestEntry == null) {
            return new DataResult<>(new Prompt("", NO_BLOCK_STRING), NO_BLOCK_STRING);
        }
        String blockName = latestEntry.blockName().replace("_", " ");
        LLMBlockContext context = new LLMBlockContext(blockName, budInstance.getOwner());
        Prompt prompt = this.llmCreation.createPrompt(context, budInstance.getData().getBudMessage());
        return new DataResult<>(prompt, "Block prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        // Pick a random bud for reaction
        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("default_chat");
    }
}
