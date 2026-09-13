package logisticspipes.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two checks the async modules lean on: whether a slot still holds what was planned for, and
 * how much of it may leave in one go.
 */
class ItemUtilTest {

    @BeforeAll
    static void boot() {
        TestBootstrap.boot();
    }

    private static SinkReply replyAccepting(int maxNumberOfItems) {
        return new SinkReply(SinkReply.FixedPriority.ItemSink, 0, false, false, 0, maxNumberOfItems, null);
    }

    @Test
    void theSameItemWithTheSameDataMatches() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 4);

        assertTrue(ItemUtil.equalsWithNBT(ItemIdentifier.get(stack), stack));
    }

    @Test
    void aDifferentItemDoesNotMatch() {
        ItemIdentifier diamond = ItemIdentifier.get(new ItemStack(Items.DIAMOND));

        assertFalse(ItemUtil.equalsWithNBT(diamond, new ItemStack(Items.EMERALD)));
    }

    @Test
    void addedDataBreaksTheMatch() {
        ItemStack plain = new ItemStack(Items.DIAMOND);
        ItemIdentifier planned = ItemIdentifier.get(plain);

        ItemStack written = plain.copy();
        CompoundTag tag = new CompoundTag();
        tag.putString("marker", "changed");
        written.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        assertFalse(ItemUtil.equalsWithNBT(planned, written), "the slot changed under the plan");
    }

    @Test
    void theCountIsCappedByWhatIsThereAndWhatWasAskedFor() {
        assertEquals(4, ItemUtil.getExtractionMax(4, 10, replyAccepting(0)), "never more than the stack holds");
        assertEquals(3, ItemUtil.getExtractionMax(10, 3, replyAccepting(0)), "nor more than was asked for");
    }

    @Test
    void aSinkThatWantsALimitGetsIt() {
        assertEquals(2, ItemUtil.getExtractionMax(10, 10, replyAccepting(2)));
    }

    @Test
    void aSinkWithNoLimitIsIgnored() {
        // Zero means "no opinion", so the other two bounds decide.
        assertEquals(5, ItemUtil.getExtractionMax(5, 10, replyAccepting(0)));
    }
}
