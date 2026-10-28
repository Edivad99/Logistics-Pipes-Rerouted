package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import logisticspipes.modules.AsyncAdvancedExtractor;
import logisticspipes.modules.AsyncExtractorModule;
import logisticspipes.modules.AsyncQuicksortModule;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import logisticspipes.world.item.LPItems;

/**
 * A chassis module pulling cobblestone and dirt out of the barrel it points at. The network has a
 * sink for cobblestone and a default route that takes anything.
 */
public class ExtractorTestInstance extends GameTestInstance {

    public static final MapCodec<ExtractorTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, ExtractorTestInstance::new));

    /**
     * What an extractor without upgrades has time to move, one item per job, in a short test.
     */
    private static final int FEW = 2;
    /**
     * Gives the extractor time for a few more jobs, so items it should leave had their chance.
     */
    private static final int A_FEW_JOBS = 300;

    private static final BlockPos CHASSIS = new BlockPos(2, 1, 1);
    private static final BlockPos SINK = new BlockPos(4, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(5, 1, 1);

    private final Scenario scenario;

    public ExtractorTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var sourceBarrel = LPTestHelper.placeBarrel(helper, CHASSIS.south());
        var sinkBarrel = LPTestHelper.placeBarrel(helper, SINK.south());
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var sink = LPTestHelper.placePipe(helper, SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        sink.getLogisticsModule().filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        scenario.module.place(helper);
        var chassis = LPTestHelper.pipeAt(helper, CHASSIS, PipeLogisticsChassis.class);
        if (scenario.stackUpgrade) {
            // Extraction upgrades count in the module's own upgrade slots, not the chassis's
            var upgrades = chassis.getModuleUpgradeManager(0).getInv();
            upgrades.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(
                LPItems.upgrades.get(ItemStackExtractionUpgrade.getName()))));
            upgrades.setChanged();
        }

        int cobblestoneMoved = scenario.cobblestoneMoves ? scenario.stock : 0;
        int dirtMoved = scenario.dirtMoves ? scenario.stock : 0;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, chassis, sink);
                LPTestHelper.assertRouted(helper, chassis, defaultRoute);
                LPTestHelper.assertPowered(helper, chassis);
                LPTestHelper.assertPowered(helper, sink);
                LPTestHelper.assertPowered(helper, defaultRoute);
                // Until then the cobblestone sink is not found, and cobblestone goes to the default route
                LPTestHelper.assertInterestedIn(helper, sink, Items.COBBLESTONE);
            })
            .thenExecute(() -> {
                sourceBarrel.setItem(0, new ItemStack(Items.COBBLESTONE, scenario.stock));
                sourceBarrel.setItem(1, new ItemStack(Items.DIRT, scenario.stock));
            })
            .thenWaitUntil(() -> {
                LPTestHelper.assertCount(helper, sinkBarrel, Items.COBBLESTONE, cobblestoneMoved, "Cobblestone sink");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.DIRT, dirtMoved, "Default route");
            })
            .thenIdle(A_FEW_JOBS)
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, sinkBarrel, Items.COBBLESTONE, cobblestoneMoved, "Cobblestone sink");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.DIRT, dirtMoved, "Default route");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.COBBLESTONE, 0, "Default route");
                LPTestHelper.assertCount(helper, sourceBarrel, Items.COBBLESTONE, scenario.stock - cobblestoneMoved, "Source");
                LPTestHelper.assertCount(helper, sourceBarrel, Items.DIRT, scenario.stock - dirtMoved, "Source");
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Extractor: " + scenario.getSerializedName());
    }

    enum Module {
        EXTRACTOR,
        ADVANCED_INCLUDE,
        ADVANCED_EXCLUDE,
        QUICKSORT;

        void place(GameTestHelper helper) {
            switch (this) {
                case EXTRACTOR -> LPTestHelper.placeChassis(helper, CHASSIS, AsyncExtractorModule.getName(),
                    AsyncExtractorModule.class);
                case ADVANCED_INCLUDE, ADVANCED_EXCLUDE -> {
                    var module = LPTestHelper.placeChassis(helper, CHASSIS, AsyncAdvancedExtractor.getName(),
                        AsyncAdvancedExtractor.class);
                    module.getFilterInventory().setItem(0, new ItemStack(Items.COBBLESTONE));
                    module.getItemsIncluded().setValue(this == ADVANCED_INCLUDE);
                }
                case QUICKSORT -> LPTestHelper.placeChassis(helper, CHASSIS, AsyncQuicksortModule.getName(),
                    AsyncQuicksortModule.class);
            }
        }
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Takes everything: cobblestone to its sink, dirt to the default route.
         */
        EXTRACTOR_TAKES_ALL(Module.EXTRACTOR, FEW, false, true, true),
        /**
         * A stack extraction upgrade moves a whole stack in one go.
         */
        EXTRACTOR_STACK_UPGRADE(Module.EXTRACTOR, 64, true, true, true),
        /**
         * Takes only what its filter lists.
         */
        ADVANCED_INCLUDE(Module.ADVANCED_INCLUDE, FEW, false, true, false),
        /**
         * Takes everything but what its filter lists.
         */
        ADVANCED_EXCLUDE(Module.ADVANCED_EXCLUDE, FEW, false, false, true),
        /**
         * Sends a stack only where a sink wants it: the default route does not count, so the dirt
         * stays.
         */
        QUICKSORT_SKIPS_DEFAULT_ROUTE(Module.QUICKSORT, 16, false, true, false);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Module module;
        final int stock;
        final boolean stackUpgrade;
        final boolean cobblestoneMoves;
        final boolean dirtMoves;

        Scenario(Module module, int stock, boolean stackUpgrade, boolean cobblestoneMoves, boolean dirtMoves) {
            this.module = module;
            this.stock = stock;
            this.stackUpgrade = stackUpgrade;
            this.cobblestoneMoves = cobblestoneMoves;
            this.dirtMoves = dirtMoves;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
