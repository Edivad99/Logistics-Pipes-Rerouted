package logisticspipes.request.resources;

import java.util.BitSet;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.connection.ConnectionType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.utils.FuzzyFlag;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Guards the dispatched codec for {@link IResource}.
 *
 * <p>The three implementations are told apart by a tag, and each carries a different body, so the
 * thing worth checking is that a resource comes back as the same kind it went out as -- and that
 * the buffer is left empty, which is what catches a body that reads a field its writer never wrote.
 */
class ResourceCodecTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.boot();
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
            ConnectionType.OTHER);
    }

    private static IResource roundTrip(IResource resource) {
        RegistryFriendlyByteBuf buf = buffer();
        try {
            IResource.STREAM_CODEC.encode(buf, resource);
            IResource decoded = IResource.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes(), "the codec must consume exactly what it wrote");
            return decoded;
        } finally {
            buf.release();
        }
    }

    private static ItemIdentifierStack stack(int count) {
        return ItemIdentifierStack.getFromStack(new ItemStack(Items.STICK, count));
    }

    @Test
    void anItemResourceRoundTrips() {
        ItemResource actual = assertInstanceOf(ItemResource.class, roundTrip(new ItemResource(stack(3), null)));

        assertEquals(3, actual.getRequestedAmount());
    }

    @Test
    void aDictResourceWithNoFuzzyFlagsRoundTrips() {
        // The empty flag set is the ordinary case, and the one an encoding that reads the BitSet's
        // first byte gets wrong: an empty BitSet has no bytes at all.
        DictResource actual = assertInstanceOf(DictResource.class, roundTrip(new DictResource(stack(1), null)));

        for (FuzzyFlag flag : FuzzyFlag.values()) {
            assertEquals(false, actual.hasFuzzyFlag(flag), flag.name());
        }
    }

    @Test
    void everyFuzzyFlagSurvivesTheRoundTrip() {
        for (FuzzyFlag flag : FuzzyFlag.values()) {
            BitSet bits = new BitSet();
            bits.set(flag.getBit());
            DictResource sent = new DictResource(stack(1), null).loadFromBitSet(bits);

            DictResource actual = assertInstanceOf(DictResource.class, roundTrip(sent));

            for (FuzzyFlag other : FuzzyFlag.values()) {
                assertEquals(other == flag, actual.hasFuzzyFlag(other), flag + " -> " + other);
            }
        }
    }
}
