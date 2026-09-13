package logisticspipes.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the DataComponents-based fluid identity: interning, canonicalization, and the deterministic
 * ordering that replaced the old randomly drawn {@code uniqueID}.
 */
class FluidIdentifierTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.boot();
    }

    private static FluidIdentifier withCustomData(Fluid fluid, String key, int value) {
        FluidStack stack = new FluidStack(fluid, 1);
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return Objects.requireNonNull(FluidIdentifier.get(stack));
    }

    // -- Interning and canonicalization ---------------------------------------

    @Test
    void theSameFluidInternsToTheSameInstance() {
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(Fluids.WATER));
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(new FluidStack(Fluids.WATER, 1000)));
    }

    @Test
    void theAmountIsNotPartOfTheIdentity() {
        assertSame(
            FluidIdentifier.get(new FluidStack(Fluids.LAVA, 1)),
            FluidIdentifier.get(new FluidStack(Fluids.LAVA, 4000)));
    }

    @Test
    void theEmptyPatchInternsToTheBareIdentity() {
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(Fluids.WATER, DataComponentPatch.EMPTY));
    }

    @Test
    void anEqualPatchReachesTheSameInternedIdentity() {
        FluidIdentifier fromStack = withCustomData(Fluids.WATER, "lp_test", 3);

        // Same patch, but arriving through the Fluid + patch entry point rather than a FluidStack.
        FluidIdentifier fromPatch = FluidIdentifier.get(Fluids.WATER, fromStack.components);

        assertSame(fromStack, fromPatch);
    }

    @Test
    void anEmptyFluidStackHasNoIdentity() {
        assertNull(FluidIdentifier.get(FluidStack.EMPTY));
        assertNull(FluidIdentifier.get((FluidStack) null));
    }

    // -- FluidStack round trip ------------------------------------------------

    @Test
    void makeFluidStackRoundTripsBackToTheSameIdentity() {
        List<FluidIdentifier> identities = List.of(
            FluidIdentifier.get(Fluids.WATER),
            FluidIdentifier.get(Fluids.LAVA),
            withCustomData(Fluids.WATER, "lp_test", 7));

        for (FluidIdentifier ident : identities) {
            assertSame(ident, FluidIdentifier.get(ident.makeFluidStack(1000)), "round trip of " + ident);
        }
    }

    @Test
    void makeFluidStackCarriesTheComponentsAndTheAmount() {
        FluidIdentifier ident = withCustomData(Fluids.WATER, "lp_test", 7);

        FluidStack stack = ident.makeFluidStack(250);

        assertEquals(250, stack.getAmount());
        assertEquals(7, Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA))
            .copyTag().getIntOr("lp_test", 0));
    }

    // -- Identity -------------------------------------------------------------

    @Test
    void fluidsDifferingOnlyInComponentsAreDistinctIdentities() {
        FluidIdentifier a = withCustomData(Fluids.WATER, "lp_test", 1);
        FluidIdentifier b = withCustomData(Fluids.WATER, "lp_test", 2);

        assertNotEquals(a, b);
        assertNotEquals(a, FluidIdentifier.get(Fluids.WATER));
    }

    // -- Ordering -------------------------------------------------------------

    @Test
    void compareToIsConsistentWithEquals() {
        List<FluidIdentifier> identities = sampleIdentities();
        for (FluidIdentifier a : identities) {
            for (FluidIdentifier b : identities) {
                assertEquals(a.equals(b), a.compareTo(b) == 0,
                    "compareTo/equals disagree for " + a + " and " + b);
                assertEquals(Integer.signum(a.compareTo(b)), -Integer.signum(b.compareTo(a)),
                    "compareTo is not antisymmetric for " + a + " and " + b);
            }
        }
    }

    @Test
    void aTreeSetKeepsEveryDistinctIdentity() {
        List<FluidIdentifier> identities = sampleIdentities();

        assertEquals(new HashSet<>(identities).size(), new TreeSet<>(identities).size());
    }

    @Test
    void allListsEachFluidOnceWithoutTheFlowingVariantsOrTheEmptyFluid() {
        // Every flowing fluid is registered twice, source and flowing, and both map to the same
        // visible container -- listing both showed every fluid twice in the picker GUI.
        FluidIdentifier.initFromNeoForge(false);
        List<FluidIdentifier> all = new ArrayList<>(FluidIdentifier.all());

        assertTrue(all.contains(FluidIdentifier.get(Fluids.WATER)), "water is missing");
        assertTrue(all.contains(FluidIdentifier.get(Fluids.LAVA)), "lava is missing");
        assertTrue(all.stream().noneMatch(it -> it.getFluid() == Fluids.FLOWING_WATER),
            "flowing_water leaked into the picker");
        assertTrue(all.stream().noneMatch(it -> it.getFluid() == Fluids.FLOWING_LAVA),
            "flowing_lava leaked into the picker");
        assertTrue(all.stream().noneMatch(it -> it.getFluid() == Fluids.EMPTY),
            "the empty fluid leaked into the picker");
        assertEquals(all.size(), all.stream().distinct().count(), "all() contains duplicates");
    }

    @Test
    void allReturnsAStableSortedOrder() {
        // This drives the fluid picker GUI. It used to be HashMap iteration order, which is
        // unspecified and could differ between client and server.
        FluidIdentifier.get(Fluids.WATER);
        FluidIdentifier.get(Fluids.LAVA);
        FluidIdentifier.get(Fluids.FLOWING_WATER);

        List<FluidIdentifier> first = new ArrayList<>(FluidIdentifier.all());
        List<FluidIdentifier> second = new ArrayList<>(FluidIdentifier.all());

        assertEquals(first, second, "all() is not stable across calls");
        List<FluidIdentifier> sorted = new ArrayList<>(first);
        sorted.sort(null);
        assertEquals(sorted, first, "all() is not sorted");
        assertTrue(first.contains(FluidIdentifier.get(Fluids.WATER)));
    }

    private static List<FluidIdentifier> sampleIdentities() {
        return List.of(
            FluidIdentifier.get(Fluids.WATER),
            FluidIdentifier.get(Fluids.LAVA),
            FluidIdentifier.get(Fluids.FLOWING_WATER),
            FluidIdentifier.get(Fluids.FLOWING_LAVA),
            withCustomData(Fluids.WATER, "lp_test", 1),
            withCustomData(Fluids.WATER, "lp_test", 2),
            withCustomData(Fluids.WATER, "lp_other", 1),
            withCustomData(Fluids.LAVA, "lp_test", 1));
    }
}
