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
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.world.item.LPItems;

/**
 * A crafting pipe with nothing to craft and dirt and cobblestone left in its machine: with a
 * cleanup upgrade it sends them off to the network, as far as its cleanup filter lets it.
 */
public class CrafterCleanupTestInstance extends GameTestInstance {

    public static final MapCodec<CrafterCleanupTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, CrafterCleanupTestInstance::new));

    private static final BlockPos CRAFTER = new BlockPos(1, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(2, 1, 1);
    private static final int EACH = 8;

    private final Scenario scenario;

    public CrafterCleanupTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var machine = LPTestHelper.placeBarrel(helper, CRAFTER.south(),
            new ItemStack(Items.DIRT, EACH), new ItemStack(Items.COBBLESTONE, EACH));
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        var crafter = LPTestHelper.placePipe(helper, CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        if (scenario != Scenario.NO_UPGRADE) {
            LPTestHelper.addUpgrade(crafter, CraftingCleanupUpgrade.getName());
        }
        var module = crafter.getLogisticsModule();
        if (scenario == Scenario.EXCLUDE_COBBLESTONE || scenario == Scenario.ONLY_COBBLESTONE) {
            module.cleanupInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
            module.cleanupModeIsExclude.setValue(scenario == Scenario.EXCLUDE_COBBLESTONE);
        }

        int dirtOut = scenario.dirtLeaves ? EACH : 0;
        int cobblestoneOut = scenario.cobblestoneLeaves ? EACH : 0;
        var sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, crafter, defaultRoute);
                LPTestHelper.assertPowered(helper, crafter);
                LPTestHelper.assertPowered(helper, defaultRoute);
            });
        if (dirtOut + cobblestoneOut > 0) {
            sequence = sequence.thenWaitUntil(() -> helper.assertTrue(
                LPTestHelper.count(defaultBarrel, Items.DIRT) == dirtOut
                    && LPTestHelper.count(defaultBarrel, Items.COBBLESTONE) == cobblestoneOut,
                Component.literal("The default route should hold %d dirt and %d cobblestone, holds %d and %d".formatted(
                    dirtOut, cobblestoneOut, LPTestHelper.count(defaultBarrel, Items.DIRT),
                    LPTestHelper.count(defaultBarrel, Items.COBBLESTONE)))));
        }
        sequence
            .thenIdle(40)
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, machine, Items.DIRT, EACH - dirtOut, "Machine");
                LPTestHelper.assertCount(helper, machine, Items.COBBLESTONE, EACH - cobblestoneOut, "Machine");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.DIRT, dirtOut, "Default route");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.COBBLESTONE, cobblestoneOut, "Default route");
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Crafter Cleanup: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Without the upgrade the machine is left alone.
         */
        NO_UPGRADE(false, false),
        /**
         * An empty exclusion filter excludes nothing: everything leaves.
         */
        EVERYTHING(true, true),
        /**
         * Cobblestone excluded: only the dirt leaves.
         */
        EXCLUDE_COBBLESTONE(true, false),
        /**
         * The filter switched to include: only the cobblestone leaves.
         */
        ONLY_COBBLESTONE(false, true);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final boolean dirtLeaves;
        final boolean cobblestoneLeaves;

        Scenario(boolean dirtLeaves, boolean cobblestoneLeaves) {
            this.dirtLeaves = dirtLeaves;
            this.cobblestoneLeaves = cobblestoneLeaves;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
