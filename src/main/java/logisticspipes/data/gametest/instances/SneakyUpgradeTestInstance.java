package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.world.item.LPItems;

/**
 * A sink beside a furnace. From the side a furnace only takes fuel, into its fuel slot; a sneaky
 * upgrade set to up makes the sink insert as if from above, into the slot that is smelted.
 */
public class SneakyUpgradeTestInstance extends GameTestInstance {

    public static final MapCodec<SneakyUpgradeTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SneakyUpgradeTestInstance::new));

    private static final BlockPos HOPPER = new BlockPos(3, 2, 1);
    private static final BlockPos SINK = new BlockPos(1, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(2, 1, 1);
    private static final BlockPos ENTRY = new BlockPos(3, 1, 1);
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int COUNT = 8;

    private final Scenario scenario;

    public SneakyUpgradeTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        helper.setBlock(SINK.south(), Blocks.FURNACE);
        var furnace = helper.getBlockEntity(SINK.south(), FurnaceBlockEntity.class);
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        helper.setBlock(HOPPER, Blocks.HOPPER);
        var sink = LPTestHelper.placePipe(helper, SINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        sink.getLogisticsModule().filterInventory.setItem(0, new ItemStack(scenario.item));
        if (scenario.sneakyUp) {
            var upgrade = LPTestHelper.upgradeStack(SneakyUpgradeConfig.getName());
            var tag = new CompoundTag();
            tag.putString(SneakyUpgradeConfig.SIDE_KEY, SneakyUpgradeConfig.Sides.UP.getLpName());
            upgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            LPTestHelper.addUpgrade(sink, upgrade);
        }
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        var entry = LPTestHelper.placePipe(helper, ENTRY, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, entry, sink);
                LPTestHelper.assertRouted(helper, entry, defaultRoute);
                LPTestHelper.assertPowered(helper, sink);
                LPTestHelper.assertPowered(helper, defaultRoute);
                LPTestHelper.assertInterestedIn(helper, sink, scenario.item);
            })
            .thenExecute(() -> helper.getBlockEntity(HOPPER, HopperBlockEntity.class)
                .setItem(0, new ItemStack(scenario.item, COUNT)))
            .thenWaitUntil(() -> {
                int input = furnace.getItem(INPUT_SLOT).is(scenario.item) ? furnace.getItem(INPUT_SLOT).getCount() : 0;
                int fuel = furnace.getItem(FUEL_SLOT).is(scenario.item) ? furnace.getItem(FUEL_SLOT).getCount() : 0;
                int rejected = LPTestHelper.count(defaultBarrel, scenario.item);
                helper.assertTrue(input == scenario.input && fuel == scenario.fuel && rejected == COUNT - input - fuel,
                    Component.literal("%s: furnace input should hold %d and holds %d, fuel %d and %d, default route %d and %d"
                        .formatted(scenario.item, scenario.input, input, scenario.fuel, fuel,
                            COUNT - scenario.input - scenario.fuel, rejected)));
            })
            .thenExecute(() -> helper.assertEntityNotPresent(EntityType.ITEM))
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Sneaky Upgrade: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Coal from the side is fuel.
         */
        COAL_FROM_SIDE(Items.COAL, false, 0, COUNT),
        /**
         * Coal from above is something to smelt.
         */
        COAL_SNEAKY_UP(Items.COAL, true, COUNT, 0),
        /**
         * Raw iron is no fuel: from the side the furnace has no room for it, and it goes to the
         * default route.
         */
        RAW_IRON_FROM_SIDE(Items.RAW_IRON, false, 0, 0),
        /**
         * From above raw iron goes in to be smelted.
         */
        RAW_IRON_SNEAKY_UP(Items.RAW_IRON, true, COUNT, 0);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Item item;
        final boolean sneakyUp;
        final int input;
        final int fuel;

        Scenario(Item item, boolean sneakyUp, int input, int fuel) {
            this.item = item;
            this.sneakyUp = sneakyUp;
            this.input = input;
            this.fuel = fuel;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
