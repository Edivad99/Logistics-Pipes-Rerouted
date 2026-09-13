package network.rs485.logisticspipes.util;

import java.util.EnumSet;

import net.minecraft.ChatFormatting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUtilTest {

    private static byte[] getBytesFromInteger(int i) {
        return new byte[] { (byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i };
    }

    @Test
    void zeroIsFourZeroBytes() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, getBytesFromInteger(0));
    }

    @Test
    void theBytesComeOutMostSignificantFirst() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, getBytesFromInteger(16909060));
    }

    @Test
    void theSmallestIntegerIsOnlyItsSignBit() {
        assertArrayEquals(new byte[] { -128, 0, 0, 0 }, getBytesFromInteger(Integer.MIN_VALUE));
    }

    @Test
    void theBiggestIntegerIsEveryBitButTheSign() {
        assertArrayEquals(new byte[] { 127, -1, -1, -1 }, getBytesFromInteger(Integer.MAX_VALUE));
    }

    @Test
    void minusOneIsEveryBitSet() {
        assertArrayEquals(new byte[] { -1, -1, -1, -1 }, getBytesFromInteger(-1));
    }

    @Test
    void transformKeepsTheStartingFormattingAcrossAReset() {
        assertEquals("§7§oThis is a semi §c§oformatted§r§7§o string.",
            TextUtil.transform("This is a semi $REDformatted$RESET string.",
                EnumSet.of(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }
}
