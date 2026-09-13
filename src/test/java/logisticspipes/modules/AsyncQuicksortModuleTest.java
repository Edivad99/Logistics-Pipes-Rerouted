package logisticspipes.modules;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Pins down what the quicksort module answers without a pipe around it. Written against the Kotlin
 * version so the same assertions, unchanged, show the Java one behaves the same way.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class AsyncQuicksortModuleTest {

    @Test
    void itIsNamedAfterItsItem() {
        assertEquals("quick_sort", new AsyncQuicksortModule().getLPName());
        assertEquals("quick_sort", AsyncQuicksortModule.getName());
    }

    @Test
    void itKeepsNoProperties() {
        assertTrue(new AsyncQuicksortModule().getProperties().isEmpty());
    }

    @Test
    void aFreshModuleWaitsTheStalledDelay() {
        // Nothing has been sorted yet, so it starts out stalled and looks around rarely.
        assertEquals(24, new AsyncQuicksortModule().getEveryNthTick());
    }

    @Test
    void itTakesNoInterestInAnythingPassive() {
        AsyncQuicksortModule module = new AsyncQuicksortModule();

        assertFalse(module.receivePassive());
        assertFalse(module.hasGenericInterests());
        assertFalse(module.interestedInUndamagedID());
        assertFalse(module.interestedInAttachedInventory());
    }

    @Test
    void withoutAServiceEveryStepOfTheJobIsANoOp() {
        AsyncQuicksortModule module = new AsyncQuicksortModule();

        assertNull(module.jobSetup(), "nothing to plan without a pipe");
        assertNull(module.tickAsync(null), "and nothing to route");
        module.completeJob(null);            // must not throw
        module.runSyncWork();                // must not throw
    }
}
