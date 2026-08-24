package com.bud.core.registry;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.core.types.DayOfWeek;
import com.bud.core.types.Pronoun;
import com.bud.core.types.RestPosition;
import com.bud.core.types.WorkRole;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractYamlMessage;
import com.bud.llm.messages.BudMessage;

public class BudDefinition extends AbstractYamlMessage {

    private String id;
    private String displayName;
    private String color;
    private String npcTypeId;
    private String weaponId;
    private String armorId;
    private Pronoun pronoun;
    private DayOfWeek favoriteDay;
    private String promptKey;
    private WorkRole workRole;
    private BudSoundDefinition sounds;
    private RestPosition restPosition;

    @Nonnull
    public String getId() {
        return id != null ? Objects.requireNonNull(id.trim().toLowerCase(Locale.ROOT)) : "";
    }

    @Nonnull
    public String getDisplayName() {
        return displayName != null ? displayName : getId();
    }

    @Nonnull
    public String getColor() {
        return color != null ? color : "#FFFFFF";
    }

    @Nonnull
    public String getNpcTypeId() {
        if (npcTypeId == null) {
            throw new IllegalStateException("BudDefinition '" + id + "' is missing npcTypeId");
        }
        return npcTypeId;
    }

    @Nonnull
    public RestPosition getRestPosition() {
        return restPosition != null ? restPosition : RestPosition.NONE;
    }

    @Nonnull
    public String getWeaponId() {
        return weaponId != null ? weaponId : "";
    }

    @Nonnull
    public String getArmorId() {
        return armorId != null ? armorId : "";
    }

    @Nonnull
    public Pronoun getPronoun() {
        return pronoun != null ? pronoun : Pronoun.THEY;
    }

    @Nonnull
    public DayOfWeek getFavoriteDay() {
        return favoriteDay != null ? favoriteDay : DayOfWeek.MONDAY;
    }

    @Nonnull
    public String getPromptKey() {
        return promptKey != null ? promptKey : getDisplayName();
    }

    @Nonnull
    public WorkRole getWorkRole() {
        return workRole != null ? workRole : WorkRole.COMPANION;
    }

    @Nonnull
    public BudSoundDefinition getSounds() {
        return sounds != null ? sounds : new BudSoundDefinition();
    }

    @Nonnull
    public BudMessage getBudMessage() {
        return LLMPromptManager.getInstance().getBudMessage(getPromptKey());
    }

    @Nonnull
    public String getPronounHint() {
        return getPronoun().describe(getDisplayName());
    }

    public static BudDefinition load(Path path) {
        return loadFromFile(BudDefinition.class, path);
    }

}
