package com.bud.queue;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.reaction.state.BudState;

public record StateChangeEntry(@Nonnull BudComponent budComponent, @Nonnull BudState newState) {

}
