package com.wawane.valet.ai;

public final class ValetStateMachine {
    private ValetStateMachine() {
    }

    public static State chooseStartState(boolean talking, boolean hasActiveOrder, boolean shouldReturnToChestBeforeWork) {
        if (talking) {
            return State.IDLE;
        }
        if (!hasActiveOrder) {
            return State.RETURNING_HOME;
        }
        if (shouldReturnToChestBeforeWork) {
            return State.RETURNING;
        }
        return State.FIND_TARGET;
    }

    public static State interruptedPathState(PathPurpose purpose, boolean hasConstructionOrder, boolean hasMiningOrder, boolean hasCraftOrder, boolean hasInventorySpace, boolean hasInventoryItems) {
        return switch (purpose) {
            case CHEST -> State.RETURNING;
            case HOME -> State.RETURNING_HOME;
            case BUILD, CRAFT -> State.FIND_TARGET;
            case ORE -> interruptedWorkState(hasConstructionOrder, hasMiningOrder, hasCraftOrder, hasInventorySpace, hasInventoryItems);
        };
    }

    public static State interruptedWorkState(boolean hasConstructionOrder, boolean hasMiningOrder, boolean hasCraftOrder, boolean hasInventorySpace, boolean hasInventoryItems) {
        if (hasConstructionOrder || hasCraftOrder || hasMiningOrder && hasInventorySpace) {
            return State.FIND_TARGET;
        }
        return hasInventoryItems ? State.RETURNING : State.RETURNING_HOME;
    }

    public enum State {
        IDLE,
        FIND_TARGET,
        EXECUTING_PATH,
        MINING,
        PLACING,
        CRAFTING,
        COLLECTING,
        RETURNING_HOME,
        RETURNING,
        DEPOSITING
    }

    public enum PathPurpose {
        ORE,
        BUILD,
        CRAFT,
        CHEST,
        HOME
    }
}
