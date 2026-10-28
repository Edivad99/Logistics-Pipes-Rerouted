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
 * Asking for more than the network has is refused as a whole: nothing is sent, not even the part
 * that is there.
 */
public class MissingItemsTestInstance extends GameTestInstance {

    public static final MapCodec<MissingItemsTestInstance> CODEC =
        LPGameTestInstances.defaultCodec(MissingItemsTestInstance::new);

    public MissingItemsTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(2, 1, 2), new ItemStack(Items.COBBLESTONE, 8));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, new BlockPos(3, 1, 2));
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, new BlockPos(2, 1, 1),
            new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var requester = LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1),
            new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));

        helper.startSequence()
            .thenWaitUntil(() -> LPTestHelper.assertRouted(helper, requester, provider))
            .thenExecute(() -> helper.assertFalse(LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, 16)),
                Component.literal("The request for 16 cobblestone should have been refused, only 8 are there")))
            .thenIdle(40)
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, 0, "Requester");
                LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 8, "Provider");
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Missing Items");
    }
}
