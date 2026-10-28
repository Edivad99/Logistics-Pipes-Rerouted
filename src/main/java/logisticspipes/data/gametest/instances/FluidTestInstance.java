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
import org.jspecify.annotations.Nullable;

import logisticspipes.data.gametest.LPTestBlocks;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeFluidProvider;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.request.RequestTree;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.world.item.LPItems;

/**
 * Fluid requested from a fluid provider pipe: it leaves the provider's tank in containers that
 * travel the network like items, and the request pipe pours it into its own tank.
 */
public class FluidTestInstance extends GameTestInstance {

    public static final MapCodec<FluidTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FluidTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(4, 1, 1);
    private static final BlockPos LAVA_PROVIDER = new BlockPos(5, 1, 1);
    private static final int REQUESTED = 1000;

    private final Scenario scenario;

    public FluidTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerTank = placeTank(helper, PROVIDER.south(), new FluidStack(Fluids.WATER, scenario.water));
        var requesterTank = placeTank(helper, REQUESTER.south(), FluidStack.EMPTY);
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeFluidProvider(LPItems.PIPE_FLUID_PROVIDER.get()));
        LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER,
            new PipeFluidRequestLogistics(LPItems.PIPE_FLUID_REQUEST.get()));
        LPTestBlocks.TankBlockEntity lavaTank = null;
        PipeFluidProvider lavaProvider = null;
        if (scenario.lava > 0) {
            lavaTank = placeTank(helper, LAVA_PROVIDER.south(), new FluidStack(Fluids.LAVA, scenario.lava));
            lavaProvider = LPTestHelper.placePipe(helper, LAVA_PROVIDER,
                new PipeFluidProvider(LPItems.PIPE_FLUID_PROVIDER.get()));
        }
        var wanted = FluidIdentifier.get(scenario.wanted);
        var wantedProvider = scenario.wanted == Fluids.LAVA ? lavaProvider : provider;
        final var finalLavaProvider = lavaProvider;
        final var finalLavaTank = lavaTank;

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, provider);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertPowered(helper, requester);
                if (finalLavaProvider != null) {
                    LPTestHelper.assertRouted(helper, requester, finalLavaProvider);
                    LPTestHelper.assertPowered(helper, finalLavaProvider);
                }
                LPTestHelper.assertInterestedIn(helper, wantedProvider, wanted.getItemIdentifier());
            })
            .thenExecute(() -> {
                int granted = scenario.partial
                    ? RequestTree.requestFluidPartial(wanted, REQUESTED, requester, null)
                    : RequestTree.requestFluid(wanted, REQUESTED, requester, null) ? REQUESTED : 0;
                helper.assertTrue(granted == scenario.delivered, Component.literal(
                    "The request should have been granted %d mB, was granted %d".formatted(scenario.delivered, granted)));
            });
        if (scenario.delivered > 0) {
            sequence = sequence.thenWaitUntil(
                () -> assertTank(helper, requesterTank, scenario.wanted, scenario.delivered, "Requester"));
        } else {
            sequence = sequence.thenIdle(60)
                .thenExecute(() -> assertTank(helper, requesterTank, scenario.wanted, 0, "Requester"));
        }
        sequence
            .thenExecute(() -> {
                int waterTaken = scenario.wanted == Fluids.WATER ? scenario.delivered : 0;
                assertTank(helper, providerTank, Fluids.WATER, scenario.water - waterTaken, "Water provider");
                if (finalLavaTank != null) {
                    int lavaTaken = scenario.wanted == Fluids.LAVA ? scenario.delivered : 0;
                    assertTank(helper, finalLavaTank, Fluids.LAVA, scenario.lava - lavaTaken, "Lava provider");
                }
            })
            .thenSucceed();
    }

    private static LPTestBlocks.TankBlockEntity placeTank(GameTestHelper helper, BlockPos pos, FluidStack content) {
        helper.setBlock(pos, LPTestBlocks.TANK.get());
        var tank = helper.getBlockEntity(pos, LPTestBlocks.TankBlockEntity.class);
        if (!content.isEmpty()) {
            tank.tank.setFluid(content);
        }
        return tank;
    }

    private static void assertTank(GameTestHelper helper, LPTestBlocks.TankBlockEntity tank, Fluid fluid, int amount,
        String where) {
        var content = tank.tank.getFluid();
        boolean matches = amount == 0 ? content.isEmpty() : content.is(fluid) && content.getAmount() == amount;
        helper.assertTrue(matches, Component.literal("%s should hold %d mB of %s, holds %s".formatted(
            where, amount, fluid, describe(content))));
    }

    private static String describe(@Nullable FluidStack content) {
        return content == null || content.isEmpty() ? "nothing" : content.getAmount() + " mB of " + content.getFluid();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Fluid: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * A bucket's worth of water out of four.
         */
        PROVIDER_TO_REQUEST(4000, 0, Fluids.WATER, false, REQUESTED),
        /**
         * Half a bucket in stock: a request for the whole bucket is refused and nothing moves.
         */
        NOT_ENOUGH(500, 0, Fluids.WATER, false, 0),
        /**
         * Half a bucket in stock, asked for as much as there is: the half bucket comes.
         */
        PARTIAL(500, 0, Fluids.WATER, true, 500),
        /**
         * Lava asked for in a network with a water provider and a lava provider: only the lava
         * provider gives anything.
         */
        RIGHT_FLUID(4000, 4000, Fluids.LAVA, false, REQUESTED);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final int water;
        final int lava;
        final Fluid wanted;
        final boolean partial;
        final int delivered;

        Scenario(int water, int lava, Fluid wanted, boolean partial, int delivered) {
            this.water = water;
            this.lava = lava;
            this.wanted = wanted;
            this.partial = partial;
            this.delivered = delivered;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
