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
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.inventory.ProviderMode;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.world.item.LPItems;

/**
 * How much a provider pipe offers, by its mode and its filter. The requester asks for all the
 * cobblestone and all the dirt the barrel holds and takes whatever the provider is willing to give.
 *
 * <p>The modes are tried on {@value #STACK} cobblestone in each of the barrel's first, middle and
 * last slot. The filters on {@value #STACK} cobblestone and {@value #STACK} dirt.
 */
public class ProviderOptionsTestInstance extends GameTestInstance {

    public static final MapCodec<ProviderOptionsTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, ProviderOptionsTestInstance::new));

    static final int STACK = 10;
    private static final int FIRST = 0;
    private static final int MIDDLE = 13;
    private static final int LAST = 26;

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(4, 1, 1);

    private final Scenario scenario;

    public ProviderOptionsTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south());
        if (scenario.filter == null) {
            for (int slot : new int[] {FIRST, MIDDLE, LAST}) {
                providerBarrel.setItem(slot, new ItemStack(Items.COBBLESTONE, STACK));
            }
        } else {
            providerBarrel.setItem(FIRST, new ItemStack(Items.COBBLESTONE, STACK));
            providerBarrel.setItem(MIDDLE, new ItemStack(Items.DIRT, STACK));
        }
        int cobblestoneStocked = LPTestHelper.count(providerBarrel, Items.COBBLESTONE);
        int dirtStocked = LPTestHelper.count(providerBarrel, Items.DIRT);
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var module = provider.getLogisticsModule();
        module.providerMode.setValue(scenario.mode);
        if (scenario.filter != null) {
            module.filterInventory.setItem(0, new ItemStack(Items.COBBLESTONE));
            module.isExclusionFilter.setValue(scenario.filter == Filter.EXCLUDE);
        }
        LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));

        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, provider);
                LPTestHelper.assertPowered(helper, provider);
            })
            .thenExecute(() -> {
                int cobblestone = cobblestoneStocked == 0 ? 0
                    : LPTestHelper.requestPartial(requester, new ItemStack(Items.COBBLESTONE, cobblestoneStocked));
                int dirt = dirtStocked == 0 ? 0
                    : LPTestHelper.requestPartial(requester, new ItemStack(Items.DIRT, dirtStocked));
                helper.assertTrue(cobblestone == scenario.cobblestone && dirt == scenario.dirt, Component.literal(
                    "The provider should offer %d cobblestone and %d dirt, offered %d and %d".formatted(
                        scenario.cobblestone, scenario.dirt, cobblestone, dirt)));
            })
            .thenWaitUntil(() -> {
                LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE, scenario.cobblestone, "Requester");
                LPTestHelper.assertCount(helper, requesterBarrel, Items.DIRT, scenario.dirt, "Requester");
            })
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE,
                    cobblestoneStocked - scenario.cobblestone, "Provider");
                LPTestHelper.assertCount(helper, providerBarrel, Items.DIRT, dirtStocked - scenario.dirt, "Provider");
                if (scenario.leftInSlots != null) {
                    assertLeftInSlots(helper, providerBarrel, scenario.leftInSlots);
                }
            })
            .thenSucceed();
    }

    private static void assertLeftInSlots(GameTestHelper helper, BarrelBlockEntity barrel, int[] expected) {
        int[] slots = {FIRST, MIDDLE, LAST};
        for (int i = 0; i < slots.length; i++) {
            int actual = barrel.getItem(slots[i]).getCount();
            helper.assertTrue(actual == expected[i], Component.literal(
                "Provider slot %d should hold %d, holds %d".formatted(slots[i], expected[i], actual)));
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Provider: " + scenario.getSerializedName());
    }

    enum Filter {
        INCLUDE,
        EXCLUDE
    }

    public enum Scenario implements StringRepresentable {
        MODE_DEFAULT(ProviderMode.DEFAULT, 3 * STACK, new int[] {0, 0, 0}),
        MODE_LEAVE_FIRST(ProviderMode.LEAVE_FIRST, 2 * STACK, new int[] {STACK, 0, 0}),
        MODE_LEAVE_LAST(ProviderMode.LEAVE_LAST, 2 * STACK, new int[] {0, 0, STACK}),
        MODE_LEAVE_FIRST_AND_LAST(ProviderMode.LEAVE_FIRST_AND_LAST, STACK, new int[] {STACK, 0, STACK}),
        MODE_LEAVE_ONE_PER_STACK(ProviderMode.LEAVE_ONE_PER_STACK, 3 * STACK - 3, new int[] {1, 1, 1}),
        /**
         * Which stack keeps the one item is the provider's business; only the total is checked.
         */
        MODE_LEAVE_ONE_PER_TYPE(ProviderMode.LEAVE_ONE_PER_TYPE, 3 * STACK - 1, null),
        /**
         * Offers only what the filter lists.
         */
        FILTER_INCLUDE(Filter.INCLUDE, STACK, 0),
        /**
         * Offers everything but what the filter lists.
         */
        FILTER_EXCLUDE(Filter.EXCLUDE, 0, STACK);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final ProviderMode mode;
        final @Nullable Filter filter;
        final int cobblestone;
        final int dirt;
        final int @Nullable [] leftInSlots;

        Scenario(ProviderMode mode, int cobblestone, int @Nullable [] leftInSlots) {
            this.mode = mode;
            this.filter = null;
            this.cobblestone = cobblestone;
            this.dirt = 0;
            this.leftInSlots = leftInSlots;
        }

        Scenario(Filter filter, int cobblestone, int dirt) {
            this.mode = ProviderMode.DEFAULT;
            this.filter = filter;
            this.cobblestone = cobblestone;
            this.dirt = dirt;
            this.leftInSlots = null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
