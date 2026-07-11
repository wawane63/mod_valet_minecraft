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

    public static State interruptedPathState(PathPurpose purpose, boolean hasConstructionOrder, boolean hasMiningOrder, boolean hasFarmOrder, boolean hasBreedingOrder, boolean hasCraftOrder, boolean hasInventorySpace, boolean hasInventoryItems) {
        return switch (purpose) {
            case CHEST -> State.RETURNING;
            case HOME -> State.RETURNING_HOME;
            case BUILD, CRAFT, COOK, STEWARD, CROP, ANIMAL -> State.FIND_TARGET;
            case ORE -> interruptedWorkState(hasConstructionOrder, hasMiningOrder, hasFarmOrder, hasBreedingOrder, hasCraftOrder, hasInventorySpace, hasInventoryItems);
        };
    }

    public static State interruptedWorkState(boolean hasConstructionOrder, boolean hasMiningOrder, boolean hasFarmOrder, boolean hasBreedingOrder, boolean hasCraftOrder, boolean hasInventorySpace, boolean hasInventoryItems) {
        if (hasConstructionOrder || hasCraftOrder || (hasMiningOrder || hasFarmOrder || hasBreedingOrder) && hasInventorySpace) {
            return State.FIND_TARGET;
        }
        return hasInventoryItems ? State.RETURNING : State.RETURNING_HOME;
    }

    public enum State {
        IDLE,
        FIND_TARGET,
        EXECUTING_PATH,
        MINING,
        HARVESTING,
        BREEDING,
        PLACING,
        CRAFTING,
        COOKING,
        STEWARDING,
        COLLECTING,
        RETURNING_HOME,
        RETURNING,
        DEPOSITING
    }

    public enum PathPurpose {
        ORE,
        CROP,
        ANIMAL,
        BUILD,
        CRAFT,
        COOK,
        STEWARD,
        CHEST,
        HOME
    }
}
