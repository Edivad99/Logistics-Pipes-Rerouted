package logisticspipes.logistics;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * How the search for a destination behaves when there is nowhere to send anything, and the order it
 * does things in: the caller's filter decides whether a lookup happens at all.
 */
class PassiveSinkFinderTest {

    @BeforeAll
    static void boot() {
        TestBootstrap.boot();
    }

    /** Built per call: stacks cannot exist before {@link TestBootstrap} has bound the components. */
    private static ItemStack diamonds() {
        return new ItemStack(Items.DIAMOND, 8);
    }

    private static ServerRouter lonelyRouter() {
        return new ServerRouter(null, Identifier.parse("logisticspipes:test"), BlockPos.ZERO);
    }

    @Test
    void withNoOneInterestedThereIsNoDestination() {
        assertNull(PassiveSinkFinder.getDestination(
            diamonds(), ItemIdentifier.get(diamonds()), false, lonelyRouter(), List.of()));
    }

    @Test
    void theFilterDecidesBeforeAnyLookupHappens() {
        AtomicInteger asked = new AtomicInteger();

        var destinations = PassiveSinkFinder.allDestinations(
            diamonds(), ItemIdentifier.get(diamonds()), true, lonelyRouter(), () -> {
                asked.incrementAndGet();
                return false;
            });

        assertFalse(destinations.hasNext(), "a filter that says no yields nothing");
        assertEquals(1, asked.get(), "and it is asked once, before looking for a destination");
    }

    @Test
    void aFilterThatSaysYesStillFindsNothingToSendTo() {
        var destinations = PassiveSinkFinder.allDestinations(
            diamonds(), ItemIdentifier.get(diamonds()), true, lonelyRouter(), () -> true);

        assertFalse(destinations.hasNext(), "nowhere to route to, however willing the caller is");
    }
}
