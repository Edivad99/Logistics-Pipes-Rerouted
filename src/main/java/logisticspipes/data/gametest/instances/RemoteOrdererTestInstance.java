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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.RemoteOrderer;

/**
 * A remote orderer linked to its pipe finds the pipe again, and what is requested through the
 * pipe lands next to it.
 */
public class RemoteOrdererTestInstance extends GameTestInstance {

    public static final MapCodec<RemoteOrdererTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, RemoteOrdererTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos ORDERER = new BlockPos(2, 1, 1);
    private static final int COBBLESTONE = 8;

    private final Scenario scenario;

    public RemoteOrdererTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.COBBLESTONE, COBBLESTONE));
        var ordererBarrel = LPTestHelper.placeBarrel(helper, ORDERER.south());
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var pipe = LPTestHelper.placePipe(helper, ORDERER,
            new PipeItemsRemoteOrdererLogistics(LPItems.PIPE_REMOTE_ORDERER.get()));
        var orderer = new ItemStack(LPItems.REMOTE_ORDERER.get());
        RemoteOrderer.connectToPipe(orderer, pipe);
        if (scenario == Scenario.AFTER_RELOAD) {
            LPTestHelper.reload(helper, ORDERER);
        }

        helper.startSequence()
            .thenWaitUntil(() -> {
                var linked = linkedPipe(helper, orderer);
                LPTestHelper.assertRouted(helper, linked, provider);
                LPTestHelper.assertPowered(helper, linked);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertInterestedIn(helper, provider, Items.COBBLESTONE);
            })
            .thenExecute(() -> helper.assertTrue(
                LPTestHelper.request(linkedPipe(helper, orderer), new ItemStack(Items.COBBLESTONE, COBBLESTONE)),
                Component.literal("The request through the remote orderer should have been accepted")))
            .thenWaitUntil(() -> LPTestHelper.assertCount(helper, ordererBarrel, Items.COBBLESTONE, COBBLESTONE,
                "Orderer"))
            .thenExecute(() -> LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 0, "Provider"))
            .thenSucceed();
    }

    private static PipeItemsRemoteOrdererLogistics linkedPipe(GameTestHelper helper, ItemStack orderer) {
        var linked = RemoteOrderer.getPipe(helper.getLevel().getServer(), orderer);
        if (linked != LPTestHelper.pipeAt(helper, ORDERER, PipeItemsRemoteOrdererLogistics.class)) {
            throw helper.assertionException(ORDERER, "The remote orderer is linked to %s, not to the pipe here", linked);
        }
        return linked;
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Remote Orderer: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The orderer is used right after it is linked.
         */
        REQUEST,
        /**
         * The pipe is reloaded after the orderer is linked, and the orderer finds the new one.
         */
        AFTER_RELOAD;

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
