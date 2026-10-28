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
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

/**
 * A chest requested from a network that holds logs: one crafting pipe turns logs into planks, the
 * other planks into the chest. Planks already in stock are used before any log is crafted.
 */
public class ChainedCraftingTestInstance extends GameTestInstance {

    public static final MapCodec<ChainedCraftingTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, ChainedCraftingTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos CHEST_CRAFTER = new BlockPos(3, 1, 1);
    private static final BlockPos PLANK_CRAFTER = new BlockPos(5, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(7, 1, 1);

    private final Scenario scenario;

    public ChainedCraftingTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        var chestTable = placeTable(helper, CHEST_CRAFTER.south());
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                chestTable.matrix.setItem(i, new ItemStack(Items.OAK_PLANKS));
            }
        }
        chestTable.cacheRecipe();
        var plankTable = placeTable(helper, PLANK_CRAFTER.south());
        plankTable.matrix.setItem(0, new ItemStack(Items.OAK_LOG));
        plankTable.cacheRecipe();

        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.OAK_PLANKS, scenario.planks), new ItemStack(Items.OAK_LOG, scenario.logs));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var chestCrafter = LPTestHelper.placePipe(helper, CHEST_CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        LPTestHelper.placePipe(helper, new BlockPos(4, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var plankCrafter = LPTestHelper.placePipe(helper, PLANK_CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        LPTestHelper.placePipe(helper, new BlockPos(6, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, chestCrafter);
                LPTestHelper.assertRouted(helper, chestCrafter, plankCrafter);
                LPTestHelper.assertRouted(helper, plankCrafter, provider);
                LPTestHelper.assertPowered(helper, chestCrafter);
                LPTestHelper.assertPowered(helper, plankCrafter);
                LPTestHelper.assertPowered(helper, provider);
            })
            .thenExecute(() -> {
                chestCrafter.getLogisticsModule().importFromCraftingTable(null);
                plankCrafter.getLogisticsModule().importFromCraftingTable(null);
            })
            .thenWaitUntil(() -> {
                LPTestHelper.assertInterestedIn(helper, chestCrafter, Items.CHEST);
                LPTestHelper.assertInterestedIn(helper, plankCrafter, Items.OAK_PLANKS);
            })
            .thenExecute(() -> {
                boolean accepted = LPTestHelper.request(requester, new ItemStack(Items.CHEST));
                helper.assertTrue(accepted == scenario.mustSucceed, Component.literal(scenario.mustSucceed
                    ? "The request for a chest should have been accepted"
                    : "The request for a chest should have been refused"));
            });

        if (scenario.mustSucceed) {
            sequence = sequence.thenWaitUntil(
                () -> LPTestHelper.assertCount(helper, requesterBarrel, Items.CHEST, 1, "Requester"));
        } else {
            sequence = sequence.thenIdle(60)
                .thenExecute(() -> LPTestHelper.assertCount(helper, requesterBarrel, Items.CHEST, 0, "Requester"));
        }
        sequence
            .thenExecute(() -> {
                LPTestHelper.assertOnly(helper, requesterBarrel, Items.CHEST, "Requester");
                LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_PLANKS, scenario.planksLeft, "Provider");
                LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_LOG, scenario.logsLeft, "Provider");
            })
            .thenSucceed();
    }

    private static LogisticsCraftingTableBlockEntity placeTable(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, LPBlocks.CRAFTER.get());
        return helper.getBlockEntity(pos, LogisticsCraftingTableBlockEntity.class);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Chained Crafting: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Two logs make exactly the eight planks the chest takes.
         */
        FROM_LOGS(0, 2, true, 0, 0),
        /**
         * Four planks in stock, so one log is crafted for the other four.
         */
        STOCK_FIRST(4, 3, true, 0, 2),
        /**
         * One log makes four planks, half of what the chest needs.
         */
        NOT_ENOUGH_LOGS(0, 1, false, 0, 1);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final int planks;
        final int logs;
        final boolean mustSucceed;
        final int planksLeft;
        final int logsLeft;

        Scenario(int planks, int logs, boolean mustSucceed, int planksLeft, int logsLeft) {
            this.planks = planks;
            this.logs = logs;
            this.mustSucceed = mustSucceed;
            this.planksLeft = planksLeft;
            this.logsLeft = logsLeft;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
