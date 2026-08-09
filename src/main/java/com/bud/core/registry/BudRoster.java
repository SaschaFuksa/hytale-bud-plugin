package com.bud.core.registry;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import com.bud.llm.messages.AbstractYamlMessage;

/**
 * The active roster of Buds used by the "create all" shorthand. Separate from which
 * {@link BudDefinition}s exist, so more than 3 Buds can be defined while only a subset
 * is offered by default.
 */
public class BudRoster extends AbstractYamlMessage {

    private List<String> defaultBuds;

    @Nonnull
    public List<String> getDefaultBuds() {
        return defaultBuds != null ? defaultBuds : List.of();
    }

    public static BudRoster load(Path path) {
        return loadFromFile(BudRoster.class, path);
    }

}
