package com.wawane.valet.group;

public enum ValetGroupMode {
    IDLE("group.valet.mode.idle"),
    FOLLOW("group.valet.mode.follow"),
    GUARD_CLOSE("group.valet.mode.guard_close"),
    GUARD_WIDE("group.valet.mode.guard_wide"),
    ATTACK_TARGET("group.valet.mode.attack_target"),
    ATTACK_AREA("group.valet.mode.attack_area"),
    RECALL("group.valet.mode.recall"),
    MOVE_TO("group.valet.mode.move_to");

    private static final ValetGroupMode[] VALUES = values();

    private final String translationKey;

    ValetGroupMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isCombatMode() {
        return this == GUARD_CLOSE || this == GUARD_WIDE || this == ATTACK_TARGET || this == ATTACK_AREA;
    }

    public static ValetGroupMode fromIndex(int index) {
        if (index < 0 || index >= VALUES.length) {
            return IDLE;
        }
        return VALUES[index];
    }
}
