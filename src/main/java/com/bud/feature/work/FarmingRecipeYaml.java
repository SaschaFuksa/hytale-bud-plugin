package com.bud.feature.work;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.llm.messages.AbstractYamlMessage;

class FarmingRecipeYaml extends AbstractYamlMessage {

    private Map<String, List<String>> allowedSeeds;

    @Nonnull
    Map<String, List<String>> getAllowedSeeds() {
        return allowedSeeds != null ? allowedSeeds : Objects.requireNonNull(Map.of());
    }

    @Nonnull
    static FarmingRecipeYaml load(@Nonnull Path path) {
        FarmingRecipeYaml loaded = loadFromFile(FarmingRecipeYaml.class, path);
        return loaded != null ? loaded : new FarmingRecipeYaml();
    }

}
