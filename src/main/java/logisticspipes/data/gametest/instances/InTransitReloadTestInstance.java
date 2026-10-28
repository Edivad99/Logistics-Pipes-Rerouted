package logisticspipes.data.gametest.instances;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
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
 * Items on their way to a requester are saved with the pipes they are in, and once those pipes
 * are loaded back the items carry on to where they were going. None is lost or dropped.
 */
public class InTransitReloadTestInstance extends GameTestInstance {

    public static final MapCodec<InTransitReloadTestInstance> CODEC =
        LPGameTestInstances.defaultCodec(InTransitReloadTestInstance::new);

    private static final int FIRST_LINK = 3;
    private static final int LAST_LINK = 6;
    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(LAST_LINK + 1, 1, 1);

    public InTransitReloadTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(), new ItemStack(Items.COBBLESTONE, 64));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        for (int x = FIRST_LINK; x <= LAST_LINK; x++) {
            LPTestHelper.placePipe(helper, new BlockPos(x, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        }
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));

        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, provider);
                LPTestHelper.assertPowered(helper, provider);
            })
            .thenExecute(() -> helper.assertTrue(LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, 64)),
                Component.literal("The request for 64 cobblestone should have been accepted")))
            .thenWaitUntil(() -> helper.assertTrue(itemsInLinks(helper) > 0,
                Component.literal("No item has reached the pipes in between yet")))
            .thenExecute(() -> {
                for (int x = FIRST_LINK; x <= LAST_LINK; x++) {
                    LPTestHelper.reload(helper, new BlockPos(x, 1, 1));
                }
            })
            .thenWaitUntil(() -> LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, 64, "Requester"))
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 0, "Provider");
                helper.assertEntityNotPresent(EntityType.ITEM);
            })
            .thenSucceed();
    }

    private static int itemsInLinks(GameTestHelper helper) {
        int count = 0;
        for (int x = FIRST_LINK; x <= LAST_LINK; x++) {
            for (var ignored : LPTestHelper.pipeAt(helper, new BlockPos(x, 1, 1), PipeItemsBasicLogistics.class).transport.items) {
                count++;
            }
        }
        return count;
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("In Transit Reload");
    }
}
