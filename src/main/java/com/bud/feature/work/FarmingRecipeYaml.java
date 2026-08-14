package com.bud.feature.work;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.llm.messages.AbstractYamlMessage;

class FarmingRecipeYaml extends AbstractYamlMessage {

    private Map<String, List<String>> allowedSeeds;

    private List<String> tillableBlocks;

    private List<String> tilledSoilBlocks;

    private String tilledSoilTargetBlock;

    @Nonnull
    Map<String, List<String>> getAllowedSeeds() {
        return allowedSeeds != null ? allowedSeeds : Objects.requireNonNull(Map.of());
    }

    @Nonnull
    List<String> getTillableBlocks() {
        return tillableBlocks != null ? tillableBlocks : Objects.requireNonNull(List.of());
    }

    @Nonnull
    List<String> getTilledSoilBlocks() {
        return tilledSoilBlocks != null ? tilledSoilBlocks : Objects.requireNonNull(List.of());
    }

    @Nullable
    String getTilledSoilTargetBlock() {
        return tilledSoilTargetBlock;
    }

    @Nonnull
    static FarmingRecipeYaml load(@Nonnull Path path) {
        FarmingRecipeYaml loaded = loadFromFile(FarmingRecipeYaml.class, path);
        return loaded != null ? loaded : new FarmingRecipeYaml();
    }

}
