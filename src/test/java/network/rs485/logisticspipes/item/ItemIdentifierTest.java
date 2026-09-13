package network.rs485.logisticspipes.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.TestBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the DataComponents-based identity model: interning, canonicalization, the projection
 * lattice, and the ordering invariant ServerRouter's TreeSet of interests depends on.
 */
class ItemIdentifierTest {

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.boot();
    }

    private static ItemIdentifier damaged(Item item, int damage) {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(damage);
        return ItemIdentifier.get(stack);
    }

    private static ItemIdentifier renamed(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return ItemIdentifier.get(stack);
    }

    private static ItemIdentifier withCustomData(Item item, String key, int value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return ItemIdentifier.get(stack);
    }

    private static ItemIdentifier potion(Holder<Potion> type) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(type));
        return ItemIdentifier.get(stack);
    }

    /** A worn, renamed pickaxe: damage plus another component, which the projections tell apart. */
    private static ItemIdentifier wornAndNamed() {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(5);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Worn"));
        return ItemIdentifier.get(stack);
    }

    // -- What the 1.12 damage field used to carry -----------------------------
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
    void formerMetadataVariantsAreDistinctItemsAndNeverCollapse() {
        ItemIdentifier white = ItemIdentifier.get(Items.WHITE_WOOL);
        ItemIdentifier red = ItemIdentifier.get(Items.RED_WOOL);

        assertNotEquals(white, red);
        // Even the most aggressive projection keeps them apart, because they differ in the Item.
        assertNotEquals(white.getUndamaged().getIgnoringNBT(), red.getUndamaged().getIgnoringNBT());
    }

    @Test
    void componentCarriedVariantsCollapseUnderGetIgnoringNBTAsTheyDidIn112() {
        // Potion type is not a counterexample to the above: it has lived in NBT since 1.9 and was
        // never metadata in 1.12.2, so getIgnoringNBT() collapsed healing and poison there too. In
        // 1.21 it is the POTION_CONTENTS component, which this projection drops -- same observable
        // behaviour. Enchanted books (STORED_ENCHANTMENTS) work the same way, but enchantments are
        // a datapack registry in 1.21 and cannot be built without a server, so they are not covered
        // here.
        ItemIdentifier healing = potion(Potions.HEALING);
        ItemIdentifier poison = potion(Potions.POISON);

        assertNotEquals(healing, poison);
        assertSame(healing.getIgnoringNBT(), poison.getIgnoringNBT());
        assertSame(ItemIdentifier.get(Items.POTION), healing.getIgnoringNBT());
    }

    // -- Interning and canonicalization ---------------------------------------

    @Test
    void equalStacksInternToTheSameInstance() {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.setDamageValue(42);

        assertSame(ItemIdentifier.get(stack), ItemIdentifier.get(stack.copy()));
    }

    @Test
    void aPatchSettingAComponentToItsPrototypeValueIsCanonicalizedAway() {
        // MAX_STACK_SIZE 64 is what cobblestone's prototype already says, so this patch describes
        // the plain item and must not produce a second identity for it.
        DataComponentPatch redundant = DataComponentPatch.builder()
            .set(DataComponents.MAX_STACK_SIZE, 64)
            .build();

        assertSame(ItemIdentifier.get(Items.COBBLESTONE), ItemIdentifier.get(Items.COBBLESTONE, redundant));
    }

    @Test
    void theEmptyPatchInternsToTheBareIdentity() {
        assertSame(ItemIdentifier.get(Items.COBBLESTONE),
            ItemIdentifier.get(Items.COBBLESTONE, DataComponentPatch.EMPTY));
    }

    // -- ItemStack round trip -------------------------------------------------

    @Test
    void makeNormalStackRoundTripsBackToTheSameIdentity() {
        List<ItemIdentifier> identities = List.of(
            ItemIdentifier.get(Items.COBBLESTONE),
            damaged(Items.DIAMOND_PICKAXE, 137),
            renamed(Items.STICK, "Pointy"),
            withCustomData(Items.STONE, "lp_test", 7),
            wornAndNamed());

        for (ItemIdentifier ident : identities) {
            assertSame(ident, ItemIdentifier.get(ident.makeNormalStack(1)), "round trip of " + ident);
        }
    }

    @Test
    void makeNormalStackCarriesDamageAndComponents() {
        ItemIdentifier ident = damaged(Items.DIAMOND_PICKAXE, 137);

        assertEquals(137, ident.makeNormalStack(1).getDamageValue());

        ItemIdentifier named = renamed(Items.STICK, "Pointy");
        assertEquals("Pointy", named.makeNormalStack(1).getHoverName().getString());
    }

    // -- Projection algebra ---------------------------------------------------

    @Test
    void getIgnoringNBTKeepsOnlyDamage() {
        ItemIdentifier projected = wornAndNamed().getIgnoringNBT();

        assertEquals(5, projected.getDamageValue());
        assertEquals(Set.of(DataComponents.DAMAGE), projected.components.entrySet().stream()
            .map(java.util.Map.Entry::getKey)
            .collect(Collectors.toSet()));
    }

    @Test
    void getUndamagedDropsDamageAndKeepsTheRest() {
        ItemIdentifier projected = wornAndNamed().getUndamaged();

        assertEquals(0, projected.getDamageValue());
        assertTrue(projected.components.entrySet().stream().noneMatch(it -> it.getKey() == DataComponents.DAMAGE));
        assertEquals("Worn", projected.makeNormalStack(1).getHoverName().getString());
    }

    @Test
    void getUndamagedIsANoOpForItemsThatCannotBeDamaged() {
        ItemIdentifier ident = withCustomData(Items.COBBLESTONE, "lp_test", 1);

        assertSame(ident, ident.getUndamaged());
    }

    @Test
    void composingBothProjectionsYieldsTheBareItem() {
        ItemIdentifier ident = wornAndNamed();

        assertTrue(ident.getUndamaged().getIgnoringNBT().components.isEmpty());
        assertSame(ItemIdentifier.get(Items.DIAMOND_PICKAXE), ident.getUndamaged().getIgnoringNBT());
    }

    @Test
    void theRoutersSixWayFanOutCollapsesToFourDistinctIdentities() {
        // Mirrors ServerRouter#getRoutersInterestedIn. getIgnoringData now coincides with
        // getUndamaged for damageable items, so two of the six are duplicates.
        ItemIdentifier ident = wornAndNamed();

        Set<ItemIdentifier> projections = new HashSet<>(List.of(
            ident,
            ident.getUndamaged(),
            ident.getIgnoringNBT(),
            ident.getUndamaged().getIgnoringNBT(),
            ident.getIgnoringData(),
            ident.getIgnoringData().getIgnoringNBT()));

        assertEquals(4, projections.size(), "projections were " + projections);
    }

    // -- Ordering -------------------------------------------------------------

    @Test
    void compareToIsConsistentWithEquals() {
        List<ItemIdentifier> identities = sampleIdentities();
        for (ItemIdentifier a : identities) {
            for (ItemIdentifier b : identities) {
                assertEquals(a.equals(b), a.compareTo(b) == 0,
                    "compareTo/equals disagree for " + a + " and " + b);
                assertEquals(Integer.signum(a.compareTo(b)), -Integer.signum(b.compareTo(a)),
                    "compareTo is not antisymmetric for " + a + " and " + b);
            }
        }
    }

    @Test
    void aTreeSetKeepsEveryDistinctIdentity() {
        // ServerRouter holds its routing interests in a TreeSet, so an order that reported two
        // distinct identities as equal would silently drop interests.
        List<ItemIdentifier> identities = sampleIdentities();

        assertEquals(new HashSet<>(identities).size(), new TreeSet<>(identities).size());
    }

    // -- Semantic comparators -------------------------------------------------

    @Test
    void equalsWithoutNBTAgreesWithComparingTheIgnoringNBTProjections() {
        List<ItemIdentifier> identities = sampleIdentities();
        for (ItemIdentifier a : identities) {
            for (ItemIdentifier b : identities) {
                assertEquals(a.getIgnoringNBT().equals(b.getIgnoringNBT()), a.equalsWithoutNBT(b),
                    "equalsWithoutNBT disagrees with the projection for " + a + " and " + b);
            }
        }
    }

    @Test
    void differentlyDamagedToolsAreDistinctIdentities() {
        assertFalse(damaged(Items.DIAMOND_PICKAXE, 1).equals(damaged(Items.DIAMOND_PICKAXE, 2)));
        assertSame(damaged(Items.DIAMOND_PICKAXE, 1).getUndamaged(),
            damaged(Items.DIAMOND_PICKAXE, 2).getUndamaged());
    }

    // -- Stack size -----------------------------------------------------------

    @Test
    void getMaxStackSizeReflectsTheItem() {
        assertEquals(64, ItemIdentifier.get(Items.COBBLESTONE).getMaxStackSize());
        assertEquals(1, ItemIdentifier.get(Items.DIAMOND_PICKAXE).getMaxStackSize());
    }

    private static List<ItemIdentifier> sampleIdentities() {
        return List.of(
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
            wornAndNamed());
    }
}
