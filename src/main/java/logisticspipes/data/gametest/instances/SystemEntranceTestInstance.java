package logisticspipes.data.gametest.instances;

import java.util.Locale;
import java.util.UUID;

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
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;

/**
 * A hopper pushes cobblestone into a system entrance pipe. Items that come in with nowhere to go
 * are sent to the system destination pipe whose card has the same frequency, past a default route
 * that is closer; without such a destination they go to the default route like anything else.
 */
public class SystemEntranceTestInstance extends GameTestInstance {

    public static final MapCodec<SystemEntranceTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SystemEntranceTestInstance::new));

    private static final BlockPos HOPPER = new BlockPos(3, 2, 1);
    private static final BlockPos DESTINATION = new BlockPos(1, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(2, 1, 1);
    private static final BlockPos ENTRANCE = new BlockPos(3, 1, 1);
    private static final int COBBLESTONE = 8;

    private final Scenario scenario;

    public SystemEntranceTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var destinationBarrel = LPTestHelper.placeBarrel(helper, DESTINATION.south());
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        helper.setBlock(HOPPER, Blocks.HOPPER);
        var destination = LPTestHelper.placePipe(helper, DESTINATION,
            new PipeItemsSystemDestinationLogistics(LPItems.PIPE_SYSTEM_DESTINATION.get()));
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        var entrance = LPTestHelper.placePipe(helper, ENTRANCE,
            new PipeItemsSystemEntranceLogistics(LPItems.PIPE_SYSTEM_ENTRANCE.get()));
        var frequency = UUID.randomUUID();
        if (scenario != Scenario.NO_CARD) {
            entrance.inv.setItem(0, card(frequency));
            destination.inv.setItem(0, card(scenario == Scenario.SAME_CARD ? frequency : UUID.randomUUID()));
        }

        int toDestination = scenario == Scenario.SAME_CARD ? COBBLESTONE : 0;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, entrance, destination);
                LPTestHelper.assertRouted(helper, entrance, defaultRoute);
                LPTestHelper.assertPowered(helper, entrance);
                LPTestHelper.assertPowered(helper, defaultRoute);
            })
            .thenExecute(() -> helper.getBlockEntity(HOPPER, HopperBlockEntity.class)
                .setItem(0, new ItemStack(Items.COBBLESTONE, COBBLESTONE)))
            .thenWaitUntil(() -> helper.assertTrue(
                LPTestHelper.count(destinationBarrel, Items.COBBLESTONE) == toDestination
                    && LPTestHelper.count(defaultBarrel, Items.COBBLESTONE) == COBBLESTONE - toDestination,
                Component.literal("Cobblestone: destination should hold %d and holds %d, default route %d and %d".formatted(
                    toDestination, LPTestHelper.count(destinationBarrel, Items.COBBLESTONE),
                    COBBLESTONE - toDestination, LPTestHelper.count(defaultBarrel, Items.COBBLESTONE)))))
            .thenExecute(() -> helper.assertEntityNotPresent(EntityType.ITEM))
            .thenSucceed();
    }

    private static ItemStack card(UUID frequency) {
        var card = new ItemStack(LPItems.ITEM_CARD.get());
        card.set(LPDataComponents.UUID.get(), frequency);
        return card;
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("System Entrance: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The entrance and the destination share a frequency.
         */
        SAME_CARD,
        /**
         * The destination listens on another frequency.
         */
        OTHER_CARD,
        /**
         * Neither pipe has a card.
         */
        NO_CARD;

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
