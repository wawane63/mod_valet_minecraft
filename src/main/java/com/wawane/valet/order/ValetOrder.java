package com.wawane.valet.order;

public enum ValetOrder {
    NONE("none", "order.valet.none"),
    MINE_ORES("mine_ores", "order.valet.mine_ores"),
    CHOP_WOOD("chop_wood", "order.valet.chop_wood"),
    BUILD_STRUCTURE("build_structure", "order.valet.build_structure");

    private final String id;
    private final String translationKey;

    ValetOrder(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static ValetOrder fromIndex(int index) {
        ValetOrder[] values = values();
        if (index < 0 || index >= values.length) {
            return NONE;
        }
        return values[index];
    }

    public static ValetOrder fromId(String id) {
        for (ValetOrder order : values()) {
            if (order.id.equals(id) || order.name().equals(id)) {
                return order;
            }
        }
        return NONE;
    }
}
