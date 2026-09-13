package network.rs485.logisticspipes.module;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.modules.AsyncExtractorModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the extractor works out from its upgrades, and what it reports about itself. Written against
 * the Kotlin version so the same assertions, unchanged, show the Java one behaves the same way.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class AsyncExtractorModuleTest {

    private final AtomicInteger actionSpeed = new AtomicInteger(0);
    private final AtomicInteger itemExtraction = new AtomicInteger(0);
    private final AtomicInteger stackExtraction = new AtomicInteger(0);

    private AsyncExtractorModule upgradedModule() {
        AsyncExtractorModule module = new AsyncExtractorModule();
        module.registerHandler(null, fakeService());
        return module;
    }

    @Test
    void itIsNamedAfterItsItem() {
        assertEquals("extractor", new AsyncExtractorModule().getLPName());
        assertEquals("extractor", AsyncExtractorModule.getName());
    }

    @Test
    void itKeepsOnlyTheSneakyDirection() {
        AsyncExtractorModule module = new AsyncExtractorModule();

        assertEquals(1, module.getProperties().size());
        assertEquals("sneakydirection", module.getProperties().getFirst().getTagKey());
        assertNull(module.getSneakyDirection(), "no sneaky direction until one is set");
    }

    @Test
    void theSneakyDirectionIsKept() {
        AsyncExtractorModule module = new AsyncExtractorModule();

        module.setSneakyDirection(Direction.UP);

        assertEquals(Direction.UP, module.getSneakyDirection());
        assertEquals(List.of("Extraction: UP"), module.getClientInformation());
    }

    @Test
    void withoutASneakyDirectionItSaysSoToTheClient() {
        assertEquals(List.of("Extraction: DEFAULT"), new AsyncExtractorModule().getClientInformation());
    }

    @Test
    void aPlainModuleExtractsOneStack() {
        AsyncExtractorModule module = upgradedModule();

        assertEquals(1, module.getStacksToExtract(), "one stack, plus one per stack upgrade");
        assertEquals(1, module.getItemsToExtract(), "and at least one item, never zero");
    }

    @Test
    void upgradesRaiseWhatItTakesInOneGo() {
        AsyncExtractorModule module = upgradedModule();
        itemExtraction.set(3);
        stackExtraction.set(2);

        assertEquals(3, module.getStacksToExtract());
        assertEquals(4 * 3 + 64 * 2, module.getItemsToExtract());
    }

    @Test
    void itTakesInterestOnlyInTheAttachedInventory() {
        AsyncExtractorModule module = new AsyncExtractorModule();

        assertTrue(module.interestedInAttachedInventory());
        assertFalse(module.receivePassive());
        assertFalse(module.hasGenericInterests());
        assertFalse(module.interestedInUndamagedID());
    }

    @Test
    void actionSpeedUpgradesShortenTheWaitBetweenJobs() {
        AsyncExtractorModule module = upgradedModule();
        int unupgraded = module.getEveryNthTick();

        actionSpeed.set(2);

        assertEquals(unupgraded - 80 + 80 / 4, module.getEveryNthTick(), "every upgrade halves the 80 tick wait");
    }

    /** Answers the upgrade counts from the fields above and nothing else. */
    private IPipeServiceProvider fakeService() {
        ISlotUpgradeManager upgrades = (ISlotUpgradeManager) Proxy.newProxyInstance(
            AsyncExtractorModuleTest.class.getClassLoader(),
            new Class<?>[] { ISlotUpgradeManager.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getActionSpeedUpgrade" -> actionSpeed.get();
                case "getItemExtractionUpgrade" -> itemExtraction.get();
                case "getItemStackExtractionUpgrade" -> stackExtraction.get();
                default -> defaultFor(method.getReturnType());
            });
        return (IPipeServiceProvider) Proxy.newProxyInstance(
            AsyncExtractorModuleTest.class.getClassLoader(),
            new Class<?>[] { IPipeServiceProvider.class },
            (proxy, method, args) -> method.getName().equals("getUpgradeManager")
                ? upgrades
                : defaultFor(method.getReturnType()));
    }

    private static Object defaultFor(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        return returnType.isPrimitive() && returnType != void.class ? 0 : null;
    }
}
