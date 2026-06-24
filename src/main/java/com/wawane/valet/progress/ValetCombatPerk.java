package com.wawane.valet.progress;

public enum ValetCombatPerk {
    SWORD_STRENGTH(ValetCombatSkillTree.SWORD, "perk.valet_combat.sword_strength", "ValetCombatPerkSwordStrength", "+"),
    SWORD_RECOVERY(ValetCombatSkillTree.SWORD, "perk.valet_combat.sword_recovery", "ValetCombatPerkSwordRecovery", "R"),
    BOW_STRENGTH(ValetCombatSkillTree.BOW, "perk.valet_combat.bow_strength", "ValetCombatPerkBowStrength", "+"),
    ALLY_AWARENESS(ValetCombatSkillTree.BOW, "perk.valet_combat.ally_awareness", "ValetCombatPerkAllyAwareness", "A");

    private final ValetCombatSkillTree tree;
    private final String translationKey;
    private final String nbtKey;
    private final String icon;

    ValetCombatPerk(ValetCombatSkillTree tree, String translationKey, String nbtKey, String icon) {
        this.tree = tree;
        this.translationKey = translationKey;
        this.nbtKey = nbtKey;
        this.icon = icon;
    }

    public ValetCombatSkillTree getTree() {
        return tree;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDescriptionKey() {
        return translationKey + ".desc";
    }

    public String getIcon() {
        return icon;
    }

    public static ValetCombatPerk fromIndex(int index) {
        ValetCombatPerk[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
