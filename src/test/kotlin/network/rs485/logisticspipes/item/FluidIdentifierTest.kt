/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */

package network.rs485.logisticspipes.item

import logisticspipes.utils.FluidIdentifier
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import org.junit.jupiter.api.BeforeAll
import java.util.TreeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the DataComponents-based fluid identity: interning, canonicalization, and the deterministic
 * ordering that replaced the old randomly drawn `uniqueID`.
 */
class FluidIdentifierTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private fun withCustomData(fluid: Fluid, key: String, value: Int): FluidIdentifier =
        FluidIdentifier.get(
            FluidStack(fluid, 1).also {
                it.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putInt(key, value) }))
            },
        )!!

    // ── Interning and canonicalization ────────────────────────────────────────

    @Test
    fun `the same fluid interns to the same instance`() {
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(Fluids.WATER))
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(FluidStack(Fluids.WATER, 1000)))
    }

    @Test
    fun `the amount is not part of the identity`() {
        assertSame(
            FluidIdentifier.get(FluidStack(Fluids.LAVA, 1)),
            FluidIdentifier.get(FluidStack(Fluids.LAVA, 4000)),
        )
    }

    @Test
    fun `the empty patch interns to the bare identity`() {
        assertSame(FluidIdentifier.get(Fluids.WATER), FluidIdentifier.get(Fluids.WATER, DataComponentPatch.EMPTY))
    }

    @Test
    fun `an equal patch reaches the same interned identity`() {
        val fromStack = withCustomData(Fluids.WATER, "lp_test", 3)
        // Same patch, but arriving through the Fluid + patch entry point rather than a FluidStack.
        val fromPatch = FluidIdentifier.get(Fluids.WATER, fromStack.components)
        assertSame(fromStack, fromPatch)
    }

    @Test
    fun `an empty fluid stack has no identity`() {
        assertNull(FluidIdentifier.get(FluidStack.EMPTY))
        assertNull(FluidIdentifier.get(null as FluidStack?))
    }

    // ── FluidStack round trip ─────────────────────────────────────────────────

    @Test
    fun `makeFluidStack round trips back to the same identity`() {
        val identities = listOf(
            FluidIdentifier.get(Fluids.WATER),
            FluidIdentifier.get(Fluids.LAVA),
            withCustomData(Fluids.WATER, "lp_test", 7),
        )
        identities.forEach { ident ->
            assertSame(ident, FluidIdentifier.get(ident.makeFluidStack(1000)), "round trip of $ident")
        }
    }

    @Test
    fun `makeFluidStack carries the components and the amount`() {
        val ident = withCustomData(Fluids.WATER, "lp_test", 7)
        val stack = ident.makeFluidStack(250)

        assertEquals(250, stack.amount)
        assertEquals(7, stack.get(DataComponents.CUSTOM_DATA)!!.copyTag().getInt("lp_test"))
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test
    fun `fluids differing only in components are distinct identities`() {
        val a = withCustomData(Fluids.WATER, "lp_test", 1)
        val b = withCustomData(Fluids.WATER, "lp_test", 2)

        assertNotEquals(a, b)
        assertNotEquals(a, FluidIdentifier.get(Fluids.WATER))
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `compareTo is consistent with equals`() {
        val identities = sampleIdentities()
        identities.forEach { a ->
            identities.forEach { b ->
                assertEquals(a == b, a.compareTo(b) == 0, "compareTo/equals disagree for $a and $b")
                assertEquals(
                    Integer.signum(a.compareTo(b)),
                    -Integer.signum(b.compareTo(a)),
                    "compareTo is not antisymmetric for $a and $b",
                )
            }
        }
    }

    @Test
    fun `a TreeSet keeps every distinct identity`() {
        val identities = sampleIdentities()
        assertEquals(identities.toSet().size, TreeSet(identities).size)
    }

    @Test
    fun `all lists each fluid once, without the flowing variants or the empty fluid`() {
        // Every flowing fluid is registered twice, source and flowing, and both map to the same
        // visible container -- listing both showed every fluid twice in the picker GUI.
        FluidIdentifier.initFromNeoForge(false)
        val all = FluidIdentifier.all().toList()

        assertTrue(all.contains(FluidIdentifier.get(Fluids.WATER)), "water is missing")
        assertTrue(all.contains(FluidIdentifier.get(Fluids.LAVA)), "lava is missing")
        assertTrue(all.none { it.fluid == Fluids.FLOWING_WATER }, "flowing_water leaked into the picker")
        assertTrue(all.none { it.fluid == Fluids.FLOWING_LAVA }, "flowing_lava leaked into the picker")
        assertTrue(all.none { it.fluid == Fluids.EMPTY }, "the empty fluid leaked into the picker")
        assertEquals(all.size, all.distinct().size, "all() contains duplicates")
    }

    @Test
    fun `all returns a stable, sorted order`() {
        // This drives the fluid picker GUI. It used to be HashMap iteration order, which is
        // unspecified and could differ between client and server.
        FluidIdentifier.get(Fluids.WATER)
        FluidIdentifier.get(Fluids.LAVA)
        FluidIdentifier.get(Fluids.FLOWING_WATER)

        val first = FluidIdentifier.all().toList()
        val second = FluidIdentifier.all().toList()

        assertEquals(first, second, "all() is not stable across calls")
        assertEquals(first.sorted(), first, "all() is not sorted")
        assertTrue(first.contains(FluidIdentifier.get(Fluids.WATER)))
    }

    private fun sampleIdentities(): List<FluidIdentifier> = listOf(
        FluidIdentifier.get(Fluids.WATER),
        FluidIdentifier.get(Fluids.LAVA),
        FluidIdentifier.get(Fluids.FLOWING_WATER),
        FluidIdentifier.get(Fluids.FLOWING_LAVA),
        withCustomData(Fluids.WATER, "lp_test", 1),
        withCustomData(Fluids.WATER, "lp_test", 2),
        withCustomData(Fluids.WATER, "lp_other", 1),
        withCustomData(Fluids.LAVA, "lp_test", 1),
    )
}
