package com.bud.profile;

import java.util.HashMap;

public class BudProfileMapper {

    private static final HashMap<BudType, IBudProfile> profileCache = new HashMap<>();

    private static final BudProfileMapper INSTANCE = new BudProfileMapper();

    private BudProfileMapper() {
        // Private constructor to prevent instantiation
    }

    public static BudProfileMapper getInstance() {
        return INSTANCE;
    }

    public IBudProfile getProfileForBudType(BudType budType) {
        return profileCache.computeIfAbsent(budType, bt -> switch (bt) {
            case GRONKH -> new GronkhProfile();
            case KEYLETH -> new KeylethProfile();
            case VERI -> new VeriProfile();
        });
    }

}
