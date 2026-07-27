package com.farmcontest.contest;

public enum ContestType {
    MOB_COMMUNITY("mob-community", "mob", true),
    MOB_SINGLE("mob-single", "mob", false),
    FARM_COMMUNITY("farm-community", "farm", true),
    FARM_SINGLE("farm-single", "farm", false);

    private final String key;
    private final String modeType;
    private final boolean community;

    ContestType(String key, String modeType, boolean community) {
        this.key = key;
        this.modeType = modeType;
        this.community = community;
    }

    public String getKey() {
        return key;
    }

    public String getModeType() {
        return modeType;
    }

    public boolean isCommunity() {
        return community;
    }

    public static ContestType fromKey(String key) {
        for (ContestType type : values()) {
            if (type.key.equalsIgnoreCase(key)) return type;
        }
        return null;
    }
}
