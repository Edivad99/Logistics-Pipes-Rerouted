package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.world.item.LPItems;

/**
 * Variations on {@link ItemSinkTestInstance}: a hopper pushes cobblestone and dirt into the
 * network, a sink filters for cobblestone and a basic pipe on the default route takes the rest.
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
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var sinkBarrel = LPTestHelper.placeBarrel(helper, SINK.south());
        if (variant == Variant.FULL_SINK) {
            for (int i = 0; i < BARREL_SLOTS; i++) {
                sinkBarrel.setItem(i, new ItemStack(Items.STONE, 64));
            }
        }
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        helper.setBlock(HOPPER, Blocks.HOPPER);
        var sink = variant.placeSink(helper);
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        var entry = LPTestHelper.placePipe(helper, ENTRY, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        int cobblestoneInSink = variant == Variant.FULL_SINK ? 0 : 8;
        int cobblestoneInDefault = 8 - cobblestoneInSink;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, entry, sink);
                LPTestHelper.assertRouted(helper, entry, defaultRoute);
                LPTestHelper.assertPowered(helper, sink);
                LPTestHelper.assertPowered(helper, defaultRoute);
                LPTestHelper.assertInterestedIn(helper, sink, Items.COBBLESTONE);
            })
            .thenExecute(() -> {
                var hopper = helper.getBlockEntity(HOPPER, HopperBlockEntity.class);
                hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
                hopper.setItem(1, new ItemStack(Items.DIRT, 8));
            })
            .thenWaitUntil(() -> helper.assertTrue(LPTestHelper.count(sinkBarrel, Items.COBBLESTONE) == cobblestoneInSink
                && LPTestHelper.count(defaultBarrel, Items.COBBLESTONE) == cobblestoneInDefault
                && LPTestHelper.count(defaultBarrel, Items.DIRT) == 8, Component.literal(
                "Cobblestone/dirt: sink %d/%d, default route %d/%d".formatted(
                    LPTestHelper.count(sinkBarrel, Items.COBBLESTONE), LPTestHelper.count(sinkBarrel, Items.DIRT),
                    LPTestHelper.count(defaultBarrel, Items.COBBLESTONE), LPTestHelper.count(defaultBarrel, Items.DIRT)))))
            .thenExecute(() -> helper.assertEntityNotPresent(EntityType.ITEM))
            .thenSucceed();
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
        CHASSIS;

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        CoreRoutedPipe placeSink(GameTestHelper helper) {
            if (this == CHASSIS) {
                LPTestHelper.placeChassis(helper, SINK, ModuleItemSink.getName(), ModuleItemSink.class)
                    .filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
                return LPTestHelper.pipeAt(helper, SINK, PipeLogisticsChassis.class);
            }
            var pipe = LPTestHelper.placePipe(helper, SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
            pipe.getLogisticsModule().filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
            return pipe;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
