package com.bud.mappings;

import java.util.HashMap;

import com.bud.profile.BudType;
import com.bud.profile.GronkhProfile;
import com.bud.profile.IBudProfile;
import com.bud.profile.KeylethProfile;
import com.bud.profile.VeriProfile;

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
