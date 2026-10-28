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

import com.mojang.serialization.MapCodec;

import logisticspipes.data.gametest.LPGameTestInstances;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.world.item.LPItems;

/**
 * A request bigger than any one provider holds is filled from two of them together.
 */
public class TwoProvidersTestInstance extends GameTestInstance {

    public static final MapCodec<TwoProvidersTestInstance> CODEC =
        LPGameTestInstances.defaultCodec(TwoProvidersTestInstance::new);

    public TwoProvidersTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var firstBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(2, 1, 2), new ItemStack(Items.COBBLESTONE, 32));
        var secondBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(3, 1, 2), new ItemStack(Items.COBBLESTONE, 32));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(4, 1, 2));
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var first = LPTestHelper.placePipe(helper, new BlockPos(2, 1, 1),
            new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var second = LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1),
            new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var requester = LPTestHelper.placePipe(helper, new BlockPos(4, 1, 1),
            new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));

        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, first);
                LPTestHelper.assertRouted(helper, requester, second);
            })
            .thenExecute(() -> helper.assertTrue(LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, 48)),
                Component.literal("The request for 48 cobblestone should have been accepted")))
            .thenWaitUntil(() -> LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, 48, "Requester"))
            .thenExecute(() -> {
                int left = LPTestHelper.count(firstBarrel, Items.COBBLESTONE)
                    + LPTestHelper.count(secondBarrel, Items.COBBLESTONE);
                helper.assertTrue(left == 16,
                    Component.literal("The providers should hold 16 cobblestone between them, hold " + left));
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Two Providers");
    }
}
