package com.wawane.valet.progress;

public enum ValetCombatSkillTree {
    SWORD("Sword"),
    BOW("Bow");

    private final String nbtPrefix;

    ValetCombatSkillTree(String nbtPrefix) {
        this.nbtPrefix = nbtPrefix;
    }

    public String getNbtPrefix() {
        return nbtPrefix;
    }
}
