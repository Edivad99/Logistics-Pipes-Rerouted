package logisticspipes.utils;

import java.util.BitSet;

import net.minecraft.nbt.CompoundTag;

import logisticspipes.api.property.IBitSet;
import logisticspipes.utils.item.ItemIdentifier;

public final class FuzzyUtil {

    private FuzzyUtil() {
    }

    public static void deserialize(IBitSet fuzzyBitSet, CompoundTag tag) {
        for (FuzzyFlag flag : FuzzyFlag.values()) {
            fuzzyBitSet.set(flag.getBit(), tag.getBooleanOr(flag.getNbtName(), false));
        }
    }

    public static void set(IBitSet fuzzyBitSet, FuzzyFlag flag, boolean value) {
        fuzzyBitSet.set(flag.getBit(), value);
    }

    public static void set(BitSet fuzzyBitSet, FuzzyFlag flag, boolean value) {
        fuzzyBitSet.set(flag.getBit(), value);
    }

    public static boolean get(IBitSet fuzzyBitSet, FuzzyFlag flag) {
        return fuzzyBitSet.get(flag.getBit());
    }

    public static boolean get(BitSet fuzzyBitSet, FuzzyFlag flag) {
        return fuzzyBitSet.get(flag.getBit());
    }

    public static FuzzyFlagger getter(IBitSet fuzzyBitSet) {
        return flag -> get(fuzzyBitSet, flag);
    }

    public static FuzzyFlagger getter(BitSet fuzzyBitSet) {
        return flag -> get(fuzzyBitSet, flag);
    }

    public static boolean fuzzyMatches(FuzzyFlagger fuzzyFlagger, ItemIdentifier firstItem, ItemIdentifier secondItem) {
        boolean useOreCategory = fuzzyFlagger.test(FuzzyFlag.USE_ORE_CATEGORY);
        if (fuzzyFlagger.test(FuzzyFlag.USE_ORE_DICT) || useOreCategory) {
            var firstDictIdent = firstItem.getDictIdentifiers();
            var secondDictIdent = secondItem.getDictIdentifiers();
            if (firstDictIdent != null && secondDictIdent != null
                && firstDictIdent.canMatch(secondDictIdent, true, useOreCategory)) {
                return true;
            }
        }
        // Compare the identifiers' projections directly rather than round-tripping through
        // ItemStacks. hasSubtypes was removed in 1.13+ (item variants became distinct items), so
        // the item check carries what metadata used to.
        if (firstItem.item != secondItem.item) {
            return false;
        }
        boolean ignoreDamage = fuzzyFlagger.test(FuzzyFlag.IGNORE_DAMAGE);
        boolean ignoreNBT = fuzzyFlagger.test(FuzzyFlag.IGNORE_NBT);
        if (ignoreDamage && ignoreNBT) {
            return true;
        }
        if (ignoreNBT) {
            // Keeps only DAMAGE, so everything else is ignored.
            return firstItem.getIgnoringNBT().equals(secondItem.getIgnoringNBT());
        }
        if (ignoreDamage) {
            // Drops DAMAGE, so everything else still has to match.
            return firstItem.getIgnoringData().equals(secondItem.getIgnoringData());
        }
        return firstItem.equals(secondItem);
    }
}
