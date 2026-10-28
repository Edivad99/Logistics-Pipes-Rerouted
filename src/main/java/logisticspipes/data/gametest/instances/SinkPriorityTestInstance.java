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
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.item.LPItems;

/**
 * Two sinks that both take cobblestone, and the one of the higher kind wins it even though it is
 * the farther of the two from where the cobblestone comes in.
 */
public class SinkPriorityTestInstance extends GameTestInstance {

    public static final MapCodec<SinkPriorityTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SinkPriorityTestInstance::new));

    private static final BlockPos HOPPER = new BlockPos(3, 2, 1);
    private static final BlockPos WINNER = new BlockPos(1, 1, 1);
    private static final BlockPos LOSER = new BlockPos(2, 1, 1);
    private static final BlockPos ENTRY = new BlockPos(3, 1, 1);
    private static final int COBBLESTONE = 8;
    /**
     * How much cobblestone the passive supplier asks for: half of what comes in, so the rest shows
     * that it stops once it has what it asked for.
     */
    private static final int PASSIVE_SUPPLIER_WANTS = COBBLESTONE / 2;

    private final Scenario scenario;

    public SinkPriorityTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var winnerBarrel = LPTestHelper.placeBarrel(helper, WINNER.south());
        var loserBarrel = LPTestHelper.placeBarrel(helper, LOSER.south());
        helper.setBlock(HOPPER, Blocks.HOPPER);
        var winner = scenario.winner.place(helper, WINNER);
        var loser = scenario.loser.place(helper, LOSER);
        var entry = LPTestHelper.placePipe(helper, ENTRY, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        int toWinner = scenario.winner == Kind.PASSIVE_SUPPLIER ? PASSIVE_SUPPLIER_WANTS : COBBLESTONE;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, entry, winner);
                LPTestHelper.assertRouted(helper, entry, loser);
                LPTestHelper.assertPowered(helper, winner);
                LPTestHelper.assertPowered(helper, loser);
                LPTestHelper.assertInterestedIn(helper, winner, Items.COBBLESTONE);
                LPTestHelper.assertInterestedIn(helper, loser, Items.COBBLESTONE);
            })
            .thenExecute(() -> helper.getBlockEntity(HOPPER, HopperBlockEntity.class)
                .setItem(0, new ItemStack(Items.COBBLESTONE, COBBLESTONE)))
            .thenWaitUntil(() -> helper.assertTrue(
                LPTestHelper.count(winnerBarrel, Items.COBBLESTONE) == toWinner
                    && LPTestHelper.count(loserBarrel, Items.COBBLESTONE) == COBBLESTONE - toWinner,
                Component.literal("Cobblestone: %s should hold %d and holds %d, %s should hold %d and holds %d".formatted(
                    scenario.winner.getSerializedName(), toWinner, LPTestHelper.count(winnerBarrel, Items.COBBLESTONE),
                    scenario.loser.getSerializedName(), COBBLESTONE - toWinner,
                    LPTestHelper.count(loserBarrel, Items.COBBLESTONE)))))
            .thenIdle(20)
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, winnerBarrel, Items.COBBLESTONE, toWinner,
                    scenario.winner.getSerializedName());
                helper.assertEntityNotPresent(EntityType.ITEM);
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Sink Priority: " + scenario.getSerializedName());
    }

    /**
     * The kinds of sink, each set up to take cobblestone.
     */
    enum Kind implements StringRepresentable {
        PASSIVE_SUPPLIER,
        ITEM_SINK,
        TERMINUS,
        TAG,
        MOD_BASED,
        DEFAULT_ROUTE;

        CoreRoutedPipe place(GameTestHelper helper, BlockPos pos) {
            switch (this) {
                case PASSIVE_SUPPLIER -> LPTestHelper.placeChassis(helper, pos, ModulePassiveSupplier.getName(),
                    ModulePassiveSupplier.class).filterInventory.setItem(0,
                    new ItemStack(Items.COBBLESTONE, PASSIVE_SUPPLIER_WANTS));
                case ITEM_SINK -> LPTestHelper.placeChassis(helper, pos, ModuleItemSink.getName(), ModuleItemSink.class)
                    .filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
                case TERMINUS -> LPTestHelper.placeChassis(helper, pos, ModuleTerminus.getName(), ModuleTerminus.class)
                    .filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
                case TAG -> LPTestHelper.placeChassis(helper, pos, ModuleOreDictItemSink.getName(),
                    ModuleOreDictItemSink.class).getOreList().add("c:cobblestones");
                case MOD_BASED -> LPTestHelper.placeChassis(helper, pos, ModuleModBasedItemSink.getName(),
                    ModuleModBasedItemSink.class).modList.add(ItemIdentifier.get(Items.COBBLESTONE).getModName());
                case DEFAULT_ROUTE -> {
                    var pipe = LPTestHelper.placePipe(helper, pos, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
                    pipe.getLogisticsModule().setDefaultRoute(true);
                    return pipe;
                }
            }
            return LPTestHelper.pipeAt(helper, pos, PipeLogisticsChassis.class);
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The passive supplier takes what it asked for, and only that: the rest goes to the sink.
         */
        PASSIVE_SUPPLIER_BEFORE_ITEM_SINK(Kind.PASSIVE_SUPPLIER, Kind.ITEM_SINK),
        ITEM_SINK_BEFORE_TERMINUS(Kind.ITEM_SINK, Kind.TERMINUS),
        TERMINUS_BEFORE_TAG(Kind.TERMINUS, Kind.TAG),
        TAG_BEFORE_MOD_BASED(Kind.TAG, Kind.MOD_BASED),
        MOD_BASED_BEFORE_DEFAULT_ROUTE(Kind.MOD_BASED, Kind.DEFAULT_ROUTE);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Kind winner;
        final Kind loser;

        Scenario(Kind winner, Kind loser) {
            this.winner = winner;
            this.loser = loser;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
