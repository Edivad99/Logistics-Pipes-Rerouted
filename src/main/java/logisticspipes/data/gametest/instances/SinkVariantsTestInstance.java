package logisticspipes.data.gametest.instances;

import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleEnchantmentSink;
import logisticspipes.modules.ModuleEnchantmentSinkMK2;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.item.LPItems;

/**
 * Variations on {@link ItemSinkTestInstance}: a hopper pushes what a sink wants and what it does
 * not into the network, the sink takes the first and a basic pipe on the default route the rest.
 */
public class SinkVariantsTestInstance extends GameTestInstance {

    public static final MapCodec<SinkVariantsTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Variant.CODEC.fieldOf("variant").forGetter(t -> t.variant),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SinkVariantsTestInstance::new));

    private static final BlockPos HOPPER = new BlockPos(3, 2, 1);
    private static final BlockPos SINK = new BlockPos(1, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(2, 1, 1);
    private static final BlockPos ENTRY = new BlockPos(3, 1, 1);
    private static final int BARREL_SLOTS = 27;

    private final Variant variant;

    public SinkVariantsTestInstance(Variant variant, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.variant = variant;
    }

    @Override
    public void run(GameTestHelper helper) {
        var wanted = variant.wanted(helper);
        var unwanted = variant.unwanted(helper);

        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var sinkBarrel = LPTestHelper.placeBarrel(helper, SINK.south());
        if (variant == Variant.FULL_SINK) {
            for (int i = 0; i < BARREL_SLOTS; i++) {
                sinkBarrel.setItem(i, new ItemStack(Items.STONE, 64));
            }
        } else if (variant == Variant.POLYMORPHIC) {
            sinkBarrel.setItem(0, wanted.copyWithCount(1));
        }
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        helper.setBlock(HOPPER, Blocks.HOPPER);
        var sink = variant.placeSink(helper, wanted);
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        var entry = LPTestHelper.placePipe(helper, ENTRY, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        int wantedInSink = (variant == Variant.FULL_SINK ? 0 : wanted.getCount())
            + (variant == Variant.POLYMORPHIC ? 1 : 0);
        int wantedInDefault = variant == Variant.FULL_SINK ? wanted.getCount() : 0;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, entry, sink);
                LPTestHelper.assertRouted(helper, entry, defaultRoute);
                LPTestHelper.assertPowered(helper, sink);
                LPTestHelper.assertPowered(helper, defaultRoute);
                LPTestHelper.assertInterestedIn(helper, sink, wanted.getItem());
            })
            .thenExecute(() -> {
                var hopper = helper.getBlockEntity(HOPPER, HopperBlockEntity.class);
                hopper.setItem(0, wanted.copy());
                for (int i = 0; i < unwanted.size(); i++) {
                    hopper.setItem(i + 1, unwanted.get(i).copy());
                }
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(count(sinkBarrel, wanted) == wantedInSink
                    && count(defaultBarrel, wanted) == wantedInDefault, Component.literal(
                    "%s: sink should hold %d and holds %d, default route should hold %d and holds %d".formatted(
                        wanted, wantedInSink, count(sinkBarrel, wanted), wantedInDefault, count(defaultBarrel, wanted))));
                for (var stack : unwanted) {
                    helper.assertTrue(count(defaultBarrel, stack) == stack.getCount(), Component.literal(
                        "%s: the default route should hold %d, holds %d; the sink holds %d".formatted(stack,
                            stack.getCount(), count(defaultBarrel, stack), count(sinkBarrel, stack))));
                }
            })
            .thenExecute(() -> helper.assertEntityNotPresent(EntityType.ITEM))
            .thenSucceed();
    }

    /**
     * How many of {@code like} the container holds, enchantments and all: an enchanted sword and a
     * plain one are different things to the enchantment sinks.
     */
    private static int count(Container container, ItemStack like) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, like)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static ItemStack enchanted(GameTestHelper helper, Item item) {
        var stack = new ItemStack(item);
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS), 1);
        return stack;
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Sink: " + variant.getSerializedName());
    }

    public enum Variant implements StringRepresentable {
        /**
         * The sink's inventory has no room, so the cobblestone falls back to the default route.
         */
        FULL_SINK,
        /**
         * The sink is an item sink module in a chassis rather than a basic pipe.
         */
        CHASSIS,
        /**
         * The polymorphic sink takes what its inventory already holds: one cobblestone.
         */
        POLYMORPHIC,
        /**
         * The mod sink takes this mod's items and leaves vanilla dirt alone.
         */
        MOD_BASED,
        /**
         * The creative tab sink takes the functional blocks tab, whose contents need the
         * world's registries to be listed (its banners have patterns), and leaves dirt alone.
         */
        CREATIVE_TAB,
        /**
         * The ore dictionary sink, which matches item tags now, takes the cobblestones tag.
         */
        TAG,
        /**
         * The enchantment sink takes an enchanted sword and leaves a plain one.
         */
        ENCHANTMENT,
        /**
         * The MK2 enchantment sink takes only the enchanted items in its filter: the enchanted iron
         * sword, not the plain one, nor an enchanted golden sword.
         */
        ENCHANTMENT_MK2;

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        ItemStack wanted(GameTestHelper helper) {
            return switch (this) {
                case MOD_BASED -> new ItemStack(LPItems.PIPE_BASIC.get(), 8);
                case CREATIVE_TAB -> new ItemStack(Items.CRAFTING_TABLE, 8);
                case ENCHANTMENT, ENCHANTMENT_MK2 -> enchanted(helper, Items.IRON_SWORD);
                default -> new ItemStack(Items.COBBLESTONE, 8);
            };
        }

        List<ItemStack> unwanted(GameTestHelper helper) {
            return switch (this) {
                case ENCHANTMENT -> List.of(new ItemStack(Items.IRON_SWORD));
                case ENCHANTMENT_MK2 -> List.of(new ItemStack(Items.IRON_SWORD), enchanted(helper, Items.GOLDEN_SWORD));
                default -> List.of(new ItemStack(Items.DIRT, 8));
            };
        }

        CoreRoutedPipe placeSink(GameTestHelper helper, ItemStack wanted) {
            switch (this) {
                case FULL_SINK -> {
                    var pipe = LPTestHelper.placePipe(helper, SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
                    pipe.getLogisticsModule().filterInventory.setItem(0, wanted.copyWithCount(1));
                    return pipe;
                }
                case CHASSIS -> LPTestHelper.placeChassis(helper, SINK, ModuleItemSink.getName(), ModuleItemSink.class)
                    .filterInventory.setItem(0, wanted.copyWithCount(1));
                case POLYMORPHIC -> LPTestHelper.placeChassis(helper, SINK, ModulePolymorphicItemSink.getName(),
                    ModulePolymorphicItemSink.class);
                // Filled in the way its screen fills it, from an item the player picks
                case MOD_BASED -> LPTestHelper.placeChassis(helper, SINK, ModuleModBasedItemSink.getName(),
                    ModuleModBasedItemSink.class).modList.add(ItemIdentifier.get(wanted).getModName());
                case CREATIVE_TAB -> LPTestHelper.placeChassis(helper, SINK, ModuleCreativeTabBasedItemSink.getName(),
                    ModuleCreativeTabBasedItemSink.class).tabList.add("functional_blocks");
                case TAG -> LPTestHelper.placeChassis(helper, SINK, ModuleOreDictItemSink.getName(),
                    ModuleOreDictItemSink.class).getOreList().add("c:cobblestones");
                case ENCHANTMENT -> LPTestHelper.placeChassis(helper, SINK, ModuleEnchantmentSink.getName(),
                    ModuleEnchantmentSink.class);
                case ENCHANTMENT_MK2 -> LPTestHelper.placeChassis(helper, SINK, ModuleEnchantmentSinkMK2.getName(),
                    ModuleEnchantmentSinkMK2.class).filterInventory.setItem(0, new ItemStack(Items.IRON_SWORD));
            }
            return LPTestHelper.pipeAt(helper, SINK, PipeLogisticsChassis.class);
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
