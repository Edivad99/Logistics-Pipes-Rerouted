/*
 * Copyright (c) 2026  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 */

package network.rs485.logisticspipes.util

import network.rs485.logisticspipes.TestBootstrap
import logisticspipes.utils.item.ItemIdentifier
import org.junit.jupiter.api.BeforeAll
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Truth table for [FuzzyUtil.fuzzyMatches] over the IGNORE_DAMAGE / IGNORE_NBT flags.
 *
 * Worth pinning: the fuzzy comparator used to round-trip both identifiers through
 * `makeNormalStack`, which was a stub returning a bare stack, so it matched any two identifiers of
 * the same item. It also treated IGNORE_DAMAGE as a no-op, because after skipping the damage check
 * it fell through to `isSameItemSameComponents`, which compares DAMAGE again.
 */
class FuzzyUtilTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            TestBootstrap.boot()
        }
    }

    private fun pickaxe(damage: Int, name: String?): ItemIdentifier =
        ItemIdentifier.get(
            ItemStack(Items.DIAMOND_PICKAXE).also { stack ->
                if (damage != 0) stack.damageValue = damage
                if (name != null) stack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
            },
        )

    private val base = pickaxe(damage = 4, name = "Alpha")
    private val differsInDamage = pickaxe(damage = 9, name = "Alpha")
    private val differsInNBT = pickaxe(damage = 4, name = "Beta")
    private val differsInBoth = pickaxe(damage = 9, name = "Beta")

    private fun flagger(vararg flags: FuzzyFlag): FuzzyFlagger =
        FuzzyUtil.getter(BitSet().also { bits -> flags.forEach { FuzzyUtil.set(bits, it, true) } })

    private fun assertMatches(
        flagger: FuzzyFlagger,
        expectedSame: Boolean,
        expectedDamage: Boolean,
        expectedNBT: Boolean,
        expectedBoth: Boolean,
        case: String,
    ) {
        assertEquals(expectedSame, FuzzyUtil.fuzzyMatches(flagger, base, base), "$case: identical")
        assertEquals(expectedDamage, FuzzyUtil.fuzzyMatches(flagger, base, differsInDamage), "$case: differs in damage")
        assertEquals(expectedNBT, FuzzyUtil.fuzzyMatches(flagger, base, differsInNBT), "$case: differs in NBT")
        assertEquals(expectedBoth, FuzzyUtil.fuzzyMatches(flagger, base, differsInBoth), "$case: differs in both")
    }

    @Test
    fun `no flags matches only the exact identity`() {
        assertMatches(
            flagger(),
            expectedSame = true, expectedDamage = false, expectedNBT = false, expectedBoth = false,
            case = "no flags",
        )
    }

    @Test
    fun `IGNORE_DAMAGE ignores damage but not the other components`() {
        assertMatches(
            flagger(FuzzyFlag.IGNORE_DAMAGE),
            expectedSame = true, expectedDamage = true, expectedNBT = false, expectedBoth = false,
            case = "IGNORE_DAMAGE",
        )
    }

    @Test
    fun `IGNORE_NBT ignores the other components but not damage`() {
        assertMatches(
            flagger(FuzzyFlag.IGNORE_NBT),
            expectedSame = true, expectedDamage = false, expectedNBT = true, expectedBoth = false,
            case = "IGNORE_NBT",
        )
    }

    @Test
    fun `both flags match any variant of the same item`() {
        assertMatches(
            flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT),
            expectedSame = true, expectedDamage = true, expectedNBT = true, expectedBoth = true,
            case = "IGNORE_DAMAGE + IGNORE_NBT",
        )
    }

    @Test
    fun `different items never match on the fuzzy flags alone`() {
        val stick = ItemIdentifier.get(Items.STICK)
        val flags = flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT)
        assertEquals(false, FuzzyUtil.fuzzyMatches(flags, base, stick))
    }

    @Test
    fun `wool colours never match, because they are distinct items`() {
        // These were one Item discriminated by metadata in 1.12, which is why damage was part of
        // the identity back then. The 1.13 flattening made them distinct Items, so the item check
        // keeps them apart no matter which fuzzy flags are set.
        val white = ItemIdentifier.get(Items.WHITE_WOOL)
        val red = ItemIdentifier.get(Items.RED_WOOL)
        val flags = flagger(FuzzyFlag.IGNORE_DAMAGE, FuzzyFlag.IGNORE_NBT)

        assertEquals(false, FuzzyUtil.fuzzyMatches(flags, white, red))
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(), white, red))
    }

    @Test
    fun `potion variants match under IGNORE_NBT but not without it`() {
        // Potion type was already NBT in 1.12, so IGNORE_NBT merged healing and poison there too.
        // In 1.21 it is the POTION_CONTENTS component, which the IGNORE_NBT projection drops.
        val healing = ItemIdentifier.get(
            ItemStack(Items.POTION).also { it.set(DataComponents.POTION_CONTENTS, PotionContents(Potions.HEALING)) },
        )
        val poison = ItemIdentifier.get(
            ItemStack(Items.POTION).also { it.set(DataComponents.POTION_CONTENTS, PotionContents(Potions.POISON)) },
        )

        assertEquals(true, FuzzyUtil.fuzzyMatches(flagger(FuzzyFlag.IGNORE_NBT), healing, poison))
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(), healing, poison))
        // IGNORE_DAMAGE alone must not merge them: potions carry no damage at all.
        assertEquals(false, FuzzyUtil.fuzzyMatches(flagger(FuzzyFlag.IGNORE_DAMAGE), healing, poison))
    }
}
