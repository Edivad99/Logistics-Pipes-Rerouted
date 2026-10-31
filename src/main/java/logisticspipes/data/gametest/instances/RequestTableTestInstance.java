package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestSequence;
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
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.world.item.LPItems;

/**
 * The request table: what it requests lands in its own inventory, it crafts from that inventory,
 * and what is put in its sorting slot is sent off to the network.
 */
public class RequestTableTestInstance extends GameTestInstance {

    public static final MapCodec<RequestTableTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, RequestTableTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos SINK = new BlockPos(2, 1, 1);
    private static final BlockPos TABLE = new BlockPos(3, 1, 1);
    private static final int COUNT = 8;

    private final Scenario scenario;

    public RequestTableTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.COBBLESTONE, COUNT), new ItemStack(Items.OAK_PLANKS, COUNT));
        var sinkBarrel = LPTestHelper.placeBarrel(helper, SINK.south());
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var sink = LPTestHelper.placePipe(helper, SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        sink.getLogisticsModule().filterInventory.setItem(0, new ItemStack(Items.DIRT));
        var table = LPTestHelper.placePipe(helper, TABLE, new PipeBlockRequestTable(LPItems.PIPE_REQUEST_TABLE.get()));

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, table, provider);
                LPTestHelper.assertRouted(helper, table, sink);
                LPTestHelper.assertPowered(helper, table);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertPowered(helper, sink);
                LPTestHelper.assertInterestedIn(helper, sink, Items.DIRT);
            });
        sequence = switch (scenario) {
            case REQUEST -> sequence
                .thenExecute(() -> helper.assertTrue(LPTestHelper.request(table, new ItemStack(Items.COBBLESTONE, COUNT)),
                    Component.literal("The request for cobblestone should have been accepted")))
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, table.inv, Items.COBBLESTONE, COUNT, "Table"))
                .thenExecute(() -> LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 0, "Provider"));
            case CRAFT -> sequence
                .thenExecute(() -> {
                    for (int i = 0; i < 9; i++) {
                        if (i != 4) {
                            table.matrix.setItem(i, new ItemStack(Items.OAK_PLANKS));
                        }
                    }
                    table.cacheRecipe();
                    helper.assertTrue(LPTestHelper.request(table, new ItemStack(Items.OAK_PLANKS, COUNT)),
                        Component.literal("The request for planks should have been accepted"));
                })
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, table.inv, Items.OAK_PLANKS, COUNT, "Table"))
                .thenExecute(() -> {
                    var result = table.getResultForClick();
                    helper.assertTrue(result.isEmpty(), Component.literal(
                        "The chest should have gone into the table's inventory, got %s back".formatted(result)));
                    LPTestHelper.assertCount(helper, table.inv, Items.CHEST, 1, "Table");
                    LPTestHelper.assertCount(helper, table.inv, Items.OAK_PLANKS, 0, "Table");
                });
            case SORT -> sequence
                .thenExecute(() -> table.toSortInv.setItem(0, new ItemStack(Items.DIRT, COUNT)))
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, sinkBarrel, Items.DIRT, COUNT, "Sink"))
                .thenExecute(() -> helper.assertTrue(table.toSortInv.getItem(0).isEmpty(),
                    Component.literal("The sorting slot should be empty")));
        };
        sequence.thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Request Table: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Requested cobblestone lands in the table's inventory.
         */
        REQUEST,
        /**
         * Requested planks are crafted into a chest from the table's inventory.
         */
        CRAFT,
        /**
         * Dirt in the sorting slot goes to the sink that wants it.
         */
        SORT;

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
