package com.wawane.valet.group;

public enum ValetGroupMode {
    IDLE("group.valet.mode.idle"),
    FOLLOW("group.valet.mode.follow"),
    GUARD_CLOSE("group.valet.mode.guard_close"),
    GUARD_WIDE("group.valet.mode.guard_wide"),
    ATTACK_TARGET("group.valet.mode.attack_target"),
    ATTACK_AREA("group.valet.mode.attack_area"),
    RECALL("group.valet.mode.recall");

    private final String translationKey;

    ValetGroupMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean hasPlayerAnchor() {
        return this == FOLLOW || this == GUARD_CLOSE || this == GUARD_WIDE;
    }

    public boolean isCombatMode() {
        return this == GUARD_CLOSE || this == GUARD_WIDE || this == ATTACK_TARGET || this == ATTACK_AREA;
    }

    public static ValetGroupMode fromIndex(int index) {
        ValetGroupMode[] values = values();
        if (index < 0 || index >= values.length) {
            return IDLE;
        }
        return values[index];
    }
}
