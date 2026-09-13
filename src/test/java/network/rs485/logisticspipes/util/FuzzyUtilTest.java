package network.rs485.logisticspipes.util;

import java.util.BitSet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.utils.FuzzyFlag;
import logisticspipes.utils.FuzzyFlagger;
import logisticspipes.utils.FuzzyUtil;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuzzyUtilTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.boot();
    }

    private static ItemIdentifier pickaxe(int damage, @Nullable String name) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        if (damage != 0) {
            stack.setDamageValue(damage);
        }
        if (name != null) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        return ItemIdentifier.get(stack);
    }

    private static ItemIdentifier base() {
        return pickaxe(4, "Alpha");
    }

    private static ItemIdentifier differsInDamage() {
        return pickaxe(9, "Alpha");
    }

    private static ItemIdentifier differsInNBT() {
        return pickaxe(4, "Beta");
    }

    private static ItemIdentifier differsInBoth() {
        return pickaxe(9, "Beta");
    }

    private static FuzzyFlagger flagger(FuzzyFlag... flags) {
        BitSet bits = new BitSet();
        for (FuzzyFlag flag : flags) {
            FuzzyUtil.set(bits, flag, true);
        }
        return FuzzyUtil.getter(bits);
    }

    private static void assertMatches(FuzzyFlagger flagger, boolean expectedSame, boolean expectedDamage,
        boolean expectedNBT, boolean expectedBoth, String testCase) {
        assertEquals(expectedSame, FuzzyUtil.fuzzyMatches(flagger, base(), base()), testCase + ": identical");
        assertEquals(expectedDamage, FuzzyUtil.fuzzyMatches(flagger, base(), differsInDamage()),
            testCase + ": differs in damage");
        assertEquals(expectedNBT, FuzzyUtil.fuzzyMatches(flagger, base(), differsInNBT()),
            testCase + ": differs in NBT");
        assertEquals(expectedBoth, FuzzyUtil.fuzzyMatches(flagger, base(), differsInBoth()),
            testCase + ": differs in both");
    }

    @Test
    void noFlagsMatchesOnlyTheExactIdentity() {
        assertMatches(flagger(), true, false, false, false, "no flags");
    }

    @Test
    void ignoreDamageIgnoresDamageButNotTheOtherComponents() {
        assertMatches(flagger(FuzzyFlag.IGNORE_DAMAGE), true, true, false, false, "IGNORE_DAMAGE");
    }

    @Test
    void ignoreNBTIgnoresTheOtherComponentsButNotDamage() {
        assertMatches(flagger(FuzzyFlag.IGNORE_NBT), true, false, true, false, "IGNORE_NBT");
    }

    @Test
    void bothFlagsMatchAnyVariantOfTheSameItem() {
        assertMatches(flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT), true, true, true, true,
            "IGNORE_DAMAGE + IGNORE_NBT");
    }

    @Test
    void differentItemsNeverMatchOnTheFuzzyFlagsAlone() {
        ItemIdentifier stick = ItemIdentifier.get(Items.STICK);
        FuzzyFlagger flags = flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT);

        assertEquals(false, FuzzyUtil.fuzzyMatches(flags, base(), stick));
    }

    @Test
    void woolColoursNeverMatchBecauseTheyAreDistinctItems() {
        // These were one Item discriminated by metadata in 1.12, which is why damage was part of
        // the identity back then. The 1.13 flattening made them distinct Items, so the item check
        // keeps them apart no matter which fuzzy flags are set.
        ItemIdentifier white = ItemIdentifier.get(Items.WHITE_WOOL);
        ItemIdentifier red = ItemIdentifier.get(Items.RED_WOOL);
        FuzzyFlagger flags = flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT);

        assertEquals(false, FuzzyUtil.fuzzyMatches(flags, white, red));
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(), white, red));
    }

    @Test
    void potionVariantsMatchUnderIgnoreNBTButNotWithoutIt() {
        // Potion type was already NBT in 1.12, so IGNORE_NBT merged healing and poison there too.
        // In 1.21 it is the POTION_CONTENTS component, which the IGNORE_NBT projection drops.
        ItemStack healingStack = new ItemStack(Items.POTION);
        healingStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
        ItemStack poisonStack = new ItemStack(Items.POTION);
        poisonStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
        ItemIdentifier healing = ItemIdentifier.get(healingStack);
        ItemIdentifier poison = ItemIdentifier.get(poisonStack);

        assertEquals(true, FuzzyUtil.fuzzyMatches(flagger(FuzzyFlag.IGNORE_NBT), healing, poison));
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(), healing, poison));
        // IGNORE_DAMAGE alone must not merge them: potions carry no damage at all.
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(FuzzyFlag.IGNORE_DAMAGE), healing, poison));
    }
}
