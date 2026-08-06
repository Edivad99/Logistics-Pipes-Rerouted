/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */

package network.rs485.logisticspipes.item

import logisticspipes.utils.item.ItemIdentifier
import net.minecraft.SharedConstants
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.component.CustomData
import org.junit.jupiter.api.BeforeAll
import java.util.TreeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the DataComponents-based identity model: interning, canonicalization, the projection
 * lattice, and the ordering invariant ServerRouter's TreeSet of interests depends on.
 */
class ItemIdentifierTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }

        fun damaged(item: Item, damage: Int): ItemIdentifier =
            ItemIdentifier.get(ItemStack(item).also { it.damageValue = damage })

        fun renamed(item: Item, name: String): ItemIdentifier =
            ItemIdentifier.get(ItemStack(item).also { it.set(DataComponents.CUSTOM_NAME, Component.literal(name)) })

        fun withCustomData(item: Item, key: String, value: Int): ItemIdentifier =
            ItemIdentifier.get(
                ItemStack(item).also {
                    it.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putInt(key, value) }))
                },
            )

        fun potion(type: Holder<Potion>): ItemIdentifier =
            ItemIdentifier.get(
                ItemStack(Items.POTION).also { it.set(DataComponents.POTION_CONTENTS, PotionContents(type)) },
            )
    }

    // ── What the 1.12 damage field used to carry ──────────────────────────────
    //
    // In 1.12 `damage` did two unrelated jobs, and both are why it was part of the identity:
    //   1. variant discriminator for items with subtypes (wool colours, dyes, planks, stone)
    //   2. durability for tools and armour
    // getIgnoringNBT() kept damage, so it kept both; getUndamaged() guarded on isDamageable(), which
    // is precisely what stopped it from collapsing red wool into white.
    //
    // In 1.21 the two jobs are carried by two different mechanisms, and the tests below pin that
    // neither is lost: the 1.13 flattening moved variants into distinct Items, which the `item ==`
    // check at the head of every projection handles for free, while durability stayed in the DAMAGE
    // component, which getIgnoringNBT() keeps and getUndamaged() drops.

    @Test
    fun `former metadata variants are distinct items and never collapse`() {
        val white = ItemIdentifier.get(Items.WHITE_WOOL)
        val red = ItemIdentifier.get(Items.RED_WOOL)

        assertNotEquals(white, red)
        // Even the most aggressive projection keeps them apart, because they differ in the Item.
        assertNotEquals(white.undamaged.ignoringNBT, red.undamaged.ignoringNBT)
    }

    @Test
    fun `component-carried variants collapse under getIgnoringNBT, as they did in 1_12`() {
        // Potion type is not a counterexample to the above: it has lived in NBT since 1.9 and was
        // never metadata in 1.12.2, so getIgnoringNBT() collapsed healing and poison there too. In
        // 1.21 it is the POTION_CONTENTS component, which this projection drops -- same observable
        // behaviour. Enchanted books (STORED_ENCHANTMENTS) work the same way, but enchantments are
        // a datapack registry in 1.21 and cannot be built without a server, so they are not covered
        // here.
        val healing = potion(Potions.HEALING)
        val poison = potion(Potions.POISON)

        assertNotEquals(healing, poison)
        assertSame(healing.ignoringNBT, poison.ignoringNBT)
        assertSame(ItemIdentifier.get(Items.POTION), healing.ignoringNBT)
    }

    // ── Interning and canonicalization ────────────────────────────────────────

    @Test
    fun `equal stacks intern to the same instance`() {
        val stack = ItemStack(Items.DIAMOND_PICKAXE).also { it.damageValue = 42 }
        assertSame(ItemIdentifier.get(stack), ItemIdentifier.get(stack.copy()))
    }

    @Test
    fun `a patch setting a component to its prototype value is canonicalized away`() {
        // MAX_STACK_SIZE 64 is what cobblestone's prototype already says, so this patch describes
        // the plain item and must not produce a second identity for it.
        val redundant = DataComponentPatch.builder()
            .set(DataComponents.MAX_STACK_SIZE, 64)
            .build()
        assertSame(ItemIdentifier.get(Items.COBBLESTONE), ItemIdentifier.get(Items.COBBLESTONE, redundant))
    }

    @Test
    fun `the empty patch interns to the bare identity`() {
        assertSame(ItemIdentifier.get(Items.COBBLESTONE), ItemIdentifier.get(Items.COBBLESTONE, DataComponentPatch.EMPTY))
    }

    // ── ItemStack round trip ──────────────────────────────────────────────────

    @Test
    fun `makeNormalStack round trips back to the same identity`() {
        val identities = listOf(
            ItemIdentifier.get(Items.COBBLESTONE),
            damaged(Items.DIAMOND_PICKAXE, 137),
            renamed(Items.STICK, "Pointy"),
            withCustomData(Items.STONE, "lp_test", 7),
            ItemIdentifier.get(
                ItemStack(Items.DIAMOND_PICKAXE).also {
                    it.damageValue = 3
                    it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
                },
            ),
        )
        identities.forEach { ident ->
            assertSame(ident, ItemIdentifier.get(ident.makeNormalStack(1)), "round trip of $ident")
        }
    }

    @Test
    fun `makeNormalStack carries damage and components`() {
        val ident = damaged(Items.DIAMOND_PICKAXE, 137)
        val stack = ident.makeNormalStack(1)
        assertEquals(137, stack.damageValue)

        val named = renamed(Items.STICK, "Pointy")
        assertEquals("Pointy", named.makeNormalStack(1).hoverName.string)
    }

    // ── Projection algebra ────────────────────────────────────────────────────

    @Test
    fun `getIgnoringNBT keeps only damage`() {
        val ident = ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also {
                it.damageValue = 5
                it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
            },
        )
        val projected = ident.ignoringNBT
        assertEquals(5, projected.damageValue)
        assertEquals(setOf(DataComponents.DAMAGE), projected.components.entrySet().map { it.key }.toSet())
    }

    @Test
    fun `getUndamaged drops damage and keeps the rest`() {
        val ident = ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also {
                it.damageValue = 5
                it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
            },
        )
        val projected = ident.undamaged
        assertEquals(0, projected.damageValue)
        assertTrue(projected.components.entrySet().none { it.key == DataComponents.DAMAGE })
        assertEquals("Worn", projected.makeNormalStack(1).hoverName.string)
    }

    @Test
    fun `getUndamaged is a no-op for items that cannot be damaged`() {
        val ident = withCustomData(Items.COBBLESTONE, "lp_test", 1)
        assertSame(ident, ident.undamaged)
    }

    @Test
    fun `composing both projections yields the bare item`() {
        val ident = ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also {
                it.damageValue = 5
                it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
            },
        )
        assertTrue(ident.undamaged.ignoringNBT.components.isEmpty)
        assertSame(ItemIdentifier.get(Items.DIAMOND_PICKAXE), ident.undamaged.ignoringNBT)
    }

    @Test
    fun `the router's six-way fan-out collapses to four distinct identities`() {
        // Mirrors ServerRouter#getRoutersInterestedIn. getIgnoringData now coincides with
        // getUndamaged for damageable items, so two of the six are duplicates.
        val ident = ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also {
                it.damageValue = 5
                it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
            },
        )
        val projections = setOf(
            ident,
            ident.undamaged,
            ident.ignoringNBT,
            ident.undamaged.ignoringNBT,
            ident.ignoringData,
            ident.ignoringData.ignoringNBT,
        )
        assertEquals(4, projections.size, "projections were $projections")
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `compareTo is consistent with equals`() {
        val identities = sampleIdentities()
        identities.forEach { a ->
            identities.forEach { b ->
                assertEquals(
                    a == b,
                    a.compareTo(b) == 0,
                    "compareTo/equals disagree for $a and $b",
                )
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
        // ServerRouter holds its routing interests in a TreeSet, so an order that reported two
        // distinct identities as equal would silently drop interests.
        val identities = sampleIdentities()
        assertEquals(identities.toSet().size, TreeSet(identities).size)
    }

    // ── Semantic comparators ──────────────────────────────────────────────────

    @Test
    fun `equalsWithoutNBT agrees with comparing the ignoringNBT projections`() {
        val identities = sampleIdentities()
        identities.forEach { a ->
            identities.forEach { b ->
                assertEquals(
                    a.ignoringNBT == b.ignoringNBT,
                    a.equalsWithoutNBT(b),
                    "equalsWithoutNBT disagrees with the projection for $a and $b",
                )
            }
        }
    }

    @Test
    fun `differently damaged tools are distinct identities`() {
        assertFalse(damaged(Items.DIAMOND_PICKAXE, 1) == damaged(Items.DIAMOND_PICKAXE, 2))
        assertSame(damaged(Items.DIAMOND_PICKAXE, 1).undamaged, damaged(Items.DIAMOND_PICKAXE, 2).undamaged)
    }

    // ── Stack size ────────────────────────────────────────────────────────────

    @Test
    fun `getMaxStackSize reflects the item`() {
        assertEquals(64, ItemIdentifier.get(Items.COBBLESTONE).maxStackSize)
        assertEquals(1, ItemIdentifier.get(Items.DIAMOND_PICKAXE).maxStackSize)
    }

    private fun sampleIdentities(): List<ItemIdentifier> = listOf(
        ItemIdentifier.get(Items.COBBLESTONE),
        ItemIdentifier.get(Items.STONE),
        ItemIdentifier.get(Items.DIAMOND_PICKAXE),
        ItemIdentifier.get(Items.STICK),
        damaged(Items.DIAMOND_PICKAXE, 1),
        damaged(Items.DIAMOND_PICKAXE, 2),
        damaged(Items.DIAMOND_PICKAXE, 200),
        damaged(Items.IRON_PICKAXE, 1),
        renamed(Items.STICK, "Pointy"),
        renamed(Items.STICK, "Blunt"),
        renamed(Items.COBBLESTONE, "Rock"),
        withCustomData(Items.STONE, "lp_test", 1),
        withCustomData(Items.STONE, "lp_test", 2),
        withCustomData(Items.STONE, "lp_other", 1),
        withCustomData(Items.COBBLESTONE, "lp_test", 1),
        ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also {
                it.damageValue = 5
                it.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"))
            },
        ),
    )
}
