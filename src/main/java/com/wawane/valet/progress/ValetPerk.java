package com.wawane.valet.progress;

public enum ValetPerk {
    SPEED("perk.valet.speed", "ValetPerkSpeed", ">"),
    VISION("perk.valet.vision", "ValetPerkVision", "O"),
    MOVEMENT("perk.valet.movement", "ValetPerkMovement", "M"),
    STORAGE("perk.valet.storage", "ValetPerkStorage", "#"),
    PATHING("perk.valet.pathing", "ValetPerkPathing", "P"),
    VEIN("perk.valet.vein", "ValetPerkVein", "F"),
    HAUL("perk.valet.haul", "ValetPerkHaul", "C"),
    LIGHTING("perk.valet.lighting", "ValetPerkLighting", "T");

    private final String translationKey;
    private final String nbtKey;
    private final String icon;

    ValetPerk(String translationKey, String nbtKey, String icon) {
        this.translationKey = translationKey;
        this.nbtKey = nbtKey;
        this.icon = icon;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDescriptionKey() {
        return translationKey + ".desc";
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public String getIcon() {
        return icon;
    }

    public static ValetPerk fromIndex(int index) {
        ValetPerk[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
