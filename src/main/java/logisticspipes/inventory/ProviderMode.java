package logisticspipes.inventory;

import lombok.Getter;

/**
 * The constants are saved/restored by their ordinal, so the order is part of the save format.
 */
public enum ProviderMode {
    DEFAULT(false, false, 0, 0),
    LEAVE_FIRST(false, false, 1, 0),
    LEAVE_LAST(false, false, 0, 1),
    LEAVE_FIRST_AND_LAST(false, false, 1, 1),
    LEAVE_ONE_PER_STACK(true, false, 0, 0),
    LEAVE_ONE_PER_TYPE(false, true, 0, 0);

    @Getter
    private final boolean hideOnePerStack;
    @Getter
    private final boolean hideOnePerType;
    @Getter
    private final int cropStart;
    @Getter
    private final int cropEnd;

    ProviderMode(boolean hideOnePerStack, boolean hideOnePerType, int cropStart, int cropEnd) {
        this.hideOnePerStack = hideOnePerStack;
        this.hideOnePerType = hideOnePerType;
        this.cropStart = cropStart;
        this.cropEnd = cropEnd;
    }
}
