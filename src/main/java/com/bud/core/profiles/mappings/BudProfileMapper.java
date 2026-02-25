package com.bud.core.profiles.mappings;

import java.util.HashMap;

import javax.annotation.Nonnull;

import com.bud.core.profiles.BudType;
import com.bud.core.profiles.GronkhProfile;
import com.bud.core.profiles.IBudProfile;
import com.bud.core.profiles.KeylethProfile;
import com.bud.core.profiles.VeriProfile;

public class BudProfileMapper {

    private static final HashMap<BudType, IBudProfile> profileMap = new HashMap<>();

    private static final BudProfileMapper INSTANCE = new BudProfileMapper();

    private BudProfileMapper() {
    }

    public static BudProfileMapper getInstance() {
        return INSTANCE;
    }

    @Nonnull
    @SuppressWarnings("null")
    public IBudProfile getProfileForBudType(BudType budType) {
        return profileMap.computeIfAbsent(budType, bt -> switch (bt) {
            case GRONKH -> new GronkhProfile();
            case KEYLETH -> new KeylethProfile();
            case VERI -> new VeriProfile();
        });
    }

}
