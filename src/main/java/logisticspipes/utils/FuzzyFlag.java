package logisticspipes.utils;

import lombok.Getter;

@Getter
public enum FuzzyFlag {
    IGNORE_DAMAGE(1, "ignore_dmg"),
    IGNORE_NBT(2, "ignore_nbt"),
    USE_ORE_DICT(0, "use_od"),
    USE_ORE_CATEGORY(3, "use_category");

    private final int bit;
    private final String nbtName;

    FuzzyFlag(int bit, String nbtName) {
        this.bit = bit;
        this.nbtName = nbtName;
    }
}
