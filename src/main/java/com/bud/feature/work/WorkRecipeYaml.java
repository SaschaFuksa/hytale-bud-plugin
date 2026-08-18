package com.bud.feature.work;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.llm.messages.AbstractYamlMessage;

class WorkRecipeYaml extends AbstractYamlMessage {

    private Map<String, List<String>> allowedSeeds;

    private Map<String, List<String>> allowedFuel;

    private Map<String, Map<String, String>> seedTargetPattern;

    private Map<String, String> seedTargetOverrides;

    private List<String> tillableBlocks;

    private List<String> tilledSoilBlocks;

    private String tilledSoilTargetBlock;

    private List<String> diggableBlocks;

    private String digRefillBlock;

    @Nonnull
    Map<String, List<String>> getAllowedSeeds() {
        return allowedSeeds != null ? allowedSeeds : Objects.requireNonNull(Map.of());
    }

    @Nonnull
    Map<String, List<String>> getAllowedFuel() {
        return allowedFuel != null ? allowedFuel : Objects.requireNonNull(Map.of());
    }

    @Nonnull
    Map<String, Map<String, String>> getSeedTargetPattern() {
        return seedTargetPattern != null ? seedTargetPattern : Objects.requireNonNull(Map.of());
    }

    @Nonnull
    Map<String, String> getSeedTargetOverrides() {
        return seedTargetOverrides != null ? seedTargetOverrides : Objects.requireNonNull(Map.of());
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
    List<String> getDiggableBlocks() {
        return diggableBlocks != null ? diggableBlocks : Objects.requireNonNull(List.of());
    }

    @Nullable
    String getDigRefillBlock() {
        return digRefillBlock;
    }


    @Nonnull
    static WorkRecipeYaml load(@Nonnull Path path) {
        WorkRecipeYaml loaded = loadFromFile(WorkRecipeYaml.class, path);
        return loaded != null ? loaded : new WorkRecipeYaml();
    }

}
