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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestBlocks;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeFluidProvider;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.world.item.LPItems;

/**
 * A fluid supplier MK2 keeps the tank it sits on stocked with water from a fluid provider, and
 * asks for nothing while what is missing is less than its minimum.
 */
public class FluidSupplierTestInstance extends GameTestInstance {

    public static final MapCodec<FluidSupplierTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FluidSupplierTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos SUPPLIER = new BlockPos(3, 1, 1);
    private static final int WATER = 8000;

    private final Scenario scenario;

    public FluidSupplierTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerTank = placeTank(helper, PROVIDER.south(), WATER);
        var supplierTank = placeTank(helper, SUPPLIER.south(), scenario.stocked);
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeFluidProvider(LPItems.PIPE_FLUID_PROVIDER.get()));
        LPTestHelper.placePipe(helper, new BlockPos(2, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var supplier = LPTestHelper.placePipe(helper, SUPPLIER,
            new PipeFluidSupplierMk2(LPItems.PIPE_FLUID_SUPPLIER_MK2.get()));
        var water = FluidIdentifier.get(Fluids.WATER);
        supplier.getDummyInventory().setItem(0, water.getItemIdentifier().makeNormalStack(1));
        supplier.changeFluidAmount(scenario.target, null);
        supplier.setMinMode(scenario.minimum);

        int supplied = scenario.expected - scenario.stocked;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, supplier, provider);
                LPTestHelper.assertPowered(helper, supplier);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertInterestedIn(helper, provider, water.getItemIdentifier());
            })
            .thenWaitUntil(() -> assertWater(helper, supplierTank, scenario.expected, "Supplier"))
            // Long enough for the supplier to ask again if it were going to
            .thenIdle(220)
            .thenExecute(() -> {
                assertWater(helper, supplierTank, scenario.expected, "Supplier");
                assertWater(helper, providerTank, WATER - supplied, "Provider");
            })
            .thenSucceed();
    }

    private static LPTestBlocks.TankBlockEntity placeTank(GameTestHelper helper, BlockPos pos, int water) {
        helper.setBlock(pos, LPTestBlocks.TANK.get());
        var tank = helper.getBlockEntity(pos, LPTestBlocks.TankBlockEntity.class);
        if (water > 0) {
            tank.tank.setFluid(new FluidStack(Fluids.WATER, water));
        }
        return tank;
    }

    private static void assertWater(GameTestHelper helper, LPTestBlocks.TankBlockEntity tank, int water, String where) {
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
        return Component.literal("Fluid Supplier: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * An empty tank is filled up to the amount.
         */
        STOCK(0, 3000, PipeFluidSupplierMk2.MinMode.ONE_BUCKET, 3000),
        /**
         * A tank with some water in it gets only what is missing.
         */
        TOP_UP(1000, 3000, PipeFluidSupplierMk2.MinMode.ONE_BUCKET, 3000),
        /**
         * Half a bucket missing is below the one bucket minimum.
         */
        BELOW_MINIMUM(1000, 1500, PipeFluidSupplierMk2.MinMode.ONE_BUCKET, 1000),
        /**
         * Without a minimum the half bucket is asked for.
         */
        NO_MINIMUM(1000, 1500, PipeFluidSupplierMk2.MinMode.NONE, 1500);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final int stocked;
        final int target;
        final PipeFluidSupplierMk2.MinMode minimum;
        final int expected;

        Scenario(int stocked, int target, PipeFluidSupplierMk2.MinMode minimum, int expected) {
            this.stocked = stocked;
            this.target = target;
            this.minimum = minimum;
            this.expected = expected;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
