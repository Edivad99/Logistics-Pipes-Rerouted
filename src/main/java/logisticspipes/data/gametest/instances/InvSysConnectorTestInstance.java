package logisticspipes.data.gametest.instances;

import java.util.Locale;
import java.util.UUID;

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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.world.item.LPItems;

/**
 * Two networks that share no pipe, each with an inventory system connector on the same barrel.
 * On one channel the connectors join the networks: what the one side requests from the other
 * goes into the barrel and comes out on the far side.
 */
public class InvSysConnectorTestInstance extends GameTestInstance {

    public static final MapCodec<InvSysConnectorTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, InvSysConnectorTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos NEAR_CONNECTOR = new BlockPos(2, 1, 1);
    private static final BlockPos SHARED = new BlockPos(3, 1, 1);
    private static final BlockPos FAR_CONNECTOR = new BlockPos(4, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(5, 1, 1);
    private static final int COBBLESTONE = 8;

    private final Scenario scenario;

    public InvSysConnectorTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        LPTestHelper.placePowerJunction(helper, new BlockPos(6, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.COBBLESTONE, COBBLESTONE));
        var shared = LPTestHelper.placeBarrel(helper, SHARED);
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var near = LPTestHelper.placePipe(helper, NEAR_CONNECTOR, new PipeItemsInvSysConnector(LPItems.PIPE_INV_SYS_CONNECTOR.get()));
        var far = LPTestHelper.placePipe(helper, FAR_CONNECTOR, new PipeItemsInvSysConnector(LPItems.PIPE_INV_SYS_CONNECTOR.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));
        var channel = UUID.randomUUID();
        near.setChannelFromClient(channel);
        far.setChannelFromClient(scenario == Scenario.SAME_CHANNEL ? channel : UUID.randomUUID());

        var sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, provider, near);
                LPTestHelper.assertRouted(helper, requester, far);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertPowered(helper, requester);
                LPTestHelper.assertInterestedIn(helper, provider, Items.COBBLESTONE);
            });
        if (scenario == Scenario.SAME_CHANNEL) {
            sequence
                .thenWaitUntil(() -> LPTestHelper.assertRouted(helper, requester, provider))
                .thenExecute(() -> helper.assertTrue(
                    LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, COBBLESTONE)),
                    Component.literal("The request across the channel should have been accepted")))
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, COBBLESTONE,
                    "Requester"))
                .thenExecute(() -> {
                    LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 0, "Provider");
                    LPTestHelper.assertCount(helper, shared, Items.COBBLESTONE, 0, "Shared barrel");
                    helper.assertEntityNotPresent(EntityType.ITEM);
                })
                .thenSucceed();
        } else {
            sequence
                .thenIdle(40)
                .thenExecute(() -> {
                    LPTestHelper.assertNotRouted(helper, requester, provider);
                    helper.assertFalse(LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, COBBLESTONE)),
                        Component.literal("Nothing should answer a request across two channels"));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, COBBLESTONE, "Provider");
                    LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, 0, "Requester");
                })
                .thenSucceed();
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Inventory System Connector: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Both connectors are on one channel, and the cobblestone crosses over.
         */
        SAME_CHANNEL,
        /**
         * Each connector is on its own channel, and the networks stay apart.
         */
        OTHER_CHANNEL;

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
