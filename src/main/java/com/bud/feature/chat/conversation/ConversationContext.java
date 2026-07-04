package com.bud.feature.chat.conversation;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.llm.prompt.IPromptContext;

public interface ConversationContext extends IPromptContext {

    @Nonnull
    String getConversationOwnerKey();

    @Nonnull
    ConversationMode getConversationMode();

    @Nonnull
    Set<String> getConversationParticipants();

    @Nonnull
    String getConversationInput();
}