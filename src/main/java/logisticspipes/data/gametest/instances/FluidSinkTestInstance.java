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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestBlocks;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeFluidBasic;
import logisticspipes.pipes.PipeFluidExtractor;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.world.item.LPItems;

/**
 * A fluid extractor pipe drains a tank of water into the network, and the basic fluid pipe whose
 * filter names water pours it into its own tank. A fluid terminus takes water only when no basic
 * fluid pipe wants it, even when it is the closer of the two.
 */
public class FluidSinkTestInstance extends GameTestInstance {

    public static final MapCodec<FluidSinkTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FluidSinkTestInstance::new));

    private static final BlockPos EXTRACTOR = new BlockPos(1, 1, 1);
    private static final BlockPos TERMINUS = new BlockPos(2, 1, 1);
    private static final BlockPos WATER_SINK = new BlockPos(3, 1, 1);
    private static final BlockPos LAVA_SINK = new BlockPos(4, 1, 1);
    private static final int WATER = 4000;

    private final Scenario scenario;

    public FluidSinkTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var sourceTank = placeTank(helper, EXTRACTOR.south());
        sourceTank.tank.setFluid(new FluidStack(Fluids.WATER, WATER));
        var lavaTank = placeTank(helper, LAVA_SINK.south());
        var extractor = LPTestHelper.placePipe(helper, EXTRACTOR, new PipeFluidExtractor(LPItems.PIPE_FLUID_EXTRACTOR.get()));
        var lavaSink = placeSink(helper, LAVA_SINK, Fluids.LAVA);
        LPTestBlocks.TankBlockEntity waterTank = null;
        PipeFluidBasic waterSink = null;
        if (scenario.waterSink) {
            waterTank = placeTank(helper, WATER_SINK.south());
            waterSink = placeSink(helper, WATER_SINK, Fluids.WATER);
        } else {
            LPTestHelper.placePipe(helper, WATER_SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        }
        LPTestBlocks.TankBlockEntity terminusTank = null;
        PipeFluidTerminus terminus = null;
        if (scenario.terminus) {
            terminusTank = placeTank(helper, TERMINUS.south());
            terminus = LPTestHelper.placePipe(helper, TERMINUS, new PipeFluidTerminus(LPItems.PIPE_FLUID_TERMINUS.get()));
            terminus.getSinkInv().setItem(0, FluidIdentifier.get(Fluids.WATER).getItemIdentifier().makeNormalStack(1));
        } else {
            LPTestHelper.placePipe(helper, TERMINUS, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        }
        final var finalWaterTank = waterTank;
        final var finalWaterSink = waterSink;
        final var finalTerminusTank = terminusTank;
        final var finalTerminus = terminus;

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, extractor, lavaSink);
                LPTestHelper.assertPowered(helper, extractor);
                LPTestHelper.assertPowered(helper, lavaSink);
                if (finalWaterSink != null) {
                    LPTestHelper.assertRouted(helper, extractor, finalWaterSink);
                    LPTestHelper.assertPowered(helper, finalWaterSink);
                }
                if (finalTerminus != null) {
                    LPTestHelper.assertRouted(helper, extractor, finalTerminus);
                    LPTestHelper.assertPowered(helper, finalTerminus);
                }
            });
        var winner = finalWaterTank != null ? finalWaterTank : finalTerminusTank;
        if (winner != null) {
            sequence = sequence
                .thenWaitUntil(() -> {
                    assertTank(helper, winner, WATER, winner == finalWaterTank ? "Water sink" : "Terminus");
                    assertTank(helper, sourceTank, 0, "Source");
                });
        } else {
            sequence = sequence.thenIdle(100);
        }
        sequence
            .thenExecute(() -> {
                assertTank(helper, lavaTank, 0, "Lava sink");
                if (finalTerminusTank != null && winner != finalTerminusTank) {
                    assertTank(helper, finalTerminusTank, 0, "Terminus");
                }
            })
            .thenSucceed();
    }

    private static LPTestBlocks.TankBlockEntity placeTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, LPTestBlocks.TANK.get());
        return helper.getBlockEntity(pos, LPTestBlocks.TankBlockEntity.class);
    }

    private static PipeFluidBasic placeSink(GameTestHelper helper, BlockPos pos, Fluid fluid) {
        var sink = LPTestHelper.placePipe(helper, pos, new PipeFluidBasic(LPItems.PIPE_FLUID_BASIC.get()));
        sink.getSinkInv().setItem(0, FluidIdentifier.get(fluid).getItemIdentifier().makeNormalStack(1));
        return sink;
    }

    private static void assertTank(GameTestHelper helper, LPTestBlocks.TankBlockEntity tank, int water, String where) {
        var content = tank.tank.getFluid();
        int actual = content.is(Fluids.WATER) ? content.getAmount() : 0;
        helper.assertTrue(actual == water && (content.isEmpty() || content.is(Fluids.WATER)), Component.literal(
            "%s should hold %d mB of water, holds %d mB of %s".formatted(where, water, content.getAmount(),
                content.getFluid())));
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Fluid Sink: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The whole tank of water ends up in the water sink's tank, none in the lava sink's.
         */
        TO_MATCHING_SINK(true, false),
        /**
         * With only a lava sink the water goes nowhere.
         */
        NO_MATCHING_SINK(false, false),
        /**
         * The water passes the closer terminus for the water sink behind it.
         */
        SINK_BEFORE_TERMINUS(true, true),
        /**
         * With no water sink the terminus takes the water.
         */
        TO_TERMINUS(false, true);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final boolean waterSink;
        final boolean terminus;

        Scenario(boolean waterSink, boolean terminus) {
            this.waterSink = waterSink;
            this.terminus = terminus;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
