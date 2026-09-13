/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
