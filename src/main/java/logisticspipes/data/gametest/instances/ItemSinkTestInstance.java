package logisticspipes.data.gametest.instances;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.world.item.LPItems;

/**
 * Items a hopper pushes into the network go to the basic pipe whose item sink filters for them,
 * and whatever no filter wants goes to the default route.
 *
 * <p>With {@code reload} every pipe is saved and loaded back before the items arrive, so the
 * filter and the default route must survive it.
 */
public class ItemSinkTestInstance extends GameTestInstance {

    public static final MapCodec<ItemSinkTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.fieldOf("reload").forGetter(t -> t.reload),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, ItemSinkTestInstance::new));

    private static final BlockPos HOPPER = new BlockPos(3, 2, 1);
    private static final BlockPos FILTERED = new BlockPos(1, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(2, 1, 1);
    private static final BlockPos ENTRY = new BlockPos(3, 1, 1);

    private final boolean reload;

    public ItemSinkTestInstance(boolean reload, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.reload = reload;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var filteredBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(1, 1, 2));
        var defaultBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(2, 1, 2));
        helper.setBlock(HOPPER, Blocks.HOPPER);
        LPTestHelper.placePipe(helper, FILTERED, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()))
            .getLogisticsModule().filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
        LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()))
            .getLogisticsModule().setDefaultRoute(true);
        LPTestHelper.placePipe(helper, ENTRY, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        var sequence = helper.startSequence().thenWaitUntil(() -> assertReady(helper));
        if (reload) {
            sequence = sequence
                .thenExecute(() -> {
                    LPTestHelper.reload(helper, FILTERED);
                    LPTestHelper.reload(helper, DEFAULT_ROUTE);
                    LPTestHelper.reload(helper, ENTRY);
                })
                .thenWaitUntil(() -> assertReady(helper));
        }
        sequence
            .thenExecute(() -> {
                var hopper = helper.getBlockEntity(HOPPER, HopperBlockEntity.class);
                hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 8));
                hopper.setItem(1, new ItemStack(Items.DIRT, 8));
            })
            .thenWaitUntil(() -> {
                var hopper = helper.getBlockEntity(HOPPER, HopperBlockEntity.class);
                helper.assertTrue(LPTestHelper.count(filteredBarrel, Items.COBBLESTONE) == 8
                    && LPTestHelper.count(defaultBarrel, Items.DIRT) == 8, Component.literal(
                    "Cobblestone/dirt: filtered sink %d/%d, default route %d/%d, hopper %d/%d".formatted(
                        LPTestHelper.count(filteredBarrel, Items.COBBLESTONE), LPTestHelper.count(filteredBarrel, Items.DIRT),
                        LPTestHelper.count(defaultBarrel, Items.COBBLESTONE), LPTestHelper.count(defaultBarrel, Items.DIRT),
                        LPTestHelper.count(hopper, Items.COBBLESTONE), LPTestHelper.count(hopper, Items.DIRT))));
            })
            .thenSucceed();
    }

    private static void assertReady(GameTestHelper helper) {
        var filtered = LPTestHelper.pipeAt(helper, FILTERED, PipeItemsBasicLogistics.class);
        var defaultRoute = LPTestHelper.pipeAt(helper, DEFAULT_ROUTE, PipeItemsBasicLogistics.class);
        var entry = LPTestHelper.pipeAt(helper, ENTRY, PipeItemsBasicLogistics.class);
        LPTestHelper.assertRouted(helper, entry, filtered);
        LPTestHelper.assertRouted(helper, entry, defaultRoute);
        LPTestHelper.assertPowered(helper, filtered);
        LPTestHelper.assertPowered(helper, defaultRoute);
        // Interests are what sends the cobblestone to the filter rather than the default route
        LPTestHelper.assertInterestedIn(helper, filtered, Items.COBBLESTONE);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal(reload ? "Item Sink After Reload" : "Item Sink");
    }
}
