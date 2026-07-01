package com.wawane.valet.progress;

import com.wawane.valet.ValetRole;

public enum ValetPerk {
    SPEED(ValetRole.ARTISAN, "perk.valet.speed", "ValetPerkSpeed", ">"),
    VISION(ValetRole.ARTISAN, "perk.valet.vision", "ValetPerkVision", "O"),
    MOVEMENT(ValetRole.ARTISAN, "perk.valet.movement", "ValetPerkMovement", "M"),
    STORAGE(ValetRole.ARTISAN, "perk.valet.storage", "ValetPerkStorage", "#"),
    PATHING(ValetRole.ARTISAN, "perk.valet.pathing", "ValetPerkPathing", "P"),
    VEIN(ValetRole.ARTISAN, "perk.valet.vein", "ValetPerkVein", "F"),
    HAUL(ValetRole.ARTISAN, "perk.valet.haul", "ValetPerkHaul", "C"),
    LIGHTING(ValetRole.ARTISAN, "perk.valet.lighting", "ValetPerkLighting", "T"),
    FARM_HANDS(ValetRole.FARMER, "perk.valet.farm_hands", "ValetPerkFarmHands", "H"),
    FARM_RANGE(ValetRole.FARMER, "perk.valet.farm_range", "ValetPerkFarmRange", "R"),
    FARM_REPLANTING(ValetRole.FARMER, "perk.valet.farm_replanting", "ValetPerkFarmReplanting", "S"),
    FARM_TILLING(ValetRole.FARMER, "perk.valet.farm_tilling", "ValetPerkFarmTilling", "T"),
    FARM_STORAGE(ValetRole.FARMER, "perk.valet.farm_storage", "ValetPerkFarmStorage", "#"),
    FARM_STEWARD(ValetRole.FARMER, "perk.valet.farm_steward", "ValetPerkFarmSteward", "C"),
    MAGIC_ICE(ValetRole.MAGICIAN, "perk.valet.magic_ice", "ValetPerkMagicIce", "I"),
    MAGIC_FANGS(ValetRole.MAGICIAN, "perk.valet.magic_fangs", "ValetPerkMagicFangs", "F"),
    MAGIC_SHATTER(ValetRole.MAGICIAN, "perk.valet.magic_shatter", "ValetPerkMagicShatter", "X"),
    MAGIC_HEAL(ValetRole.MAGICIAN, "perk.valet.magic_heal", "ValetPerkMagicHeal", "+"),
    MAGIC_REGEN_AURA(ValetRole.MAGICIAN, "perk.valet.magic_regen_aura", "ValetPerkMagicRegenAura", "A"),
    MAGIC_WARD(ValetRole.MAGICIAN, "perk.valet.magic_ward", "ValetPerkMagicWard", "B"),
    MAGIC_WEAKEN(ValetRole.MAGICIAN, "perk.valet.magic_weaken", "ValetPerkMagicWeaken", "-");

    private final ValetRole role;
    private final String translationKey;
    private final String nbtKey;
    private final String icon;

    ValetPerk(ValetRole role, String translationKey, String nbtKey, String icon) {
        this.role = role;
        this.translationKey = translationKey;
        this.nbtKey = nbtKey;
        this.icon = icon;
    }

    public ValetRole getRole() {
        return role;
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
