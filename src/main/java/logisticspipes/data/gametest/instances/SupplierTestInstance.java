package logisticspipes.data.gametest.instances;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.upgrades.PatternUpgrade;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.world.item.LPItems;

/**
 * A supplier pipe keeping the barrel next to it stocked from a provider, in every mode it has.
 *
 * <p>Without a pattern upgrade it keeps {@value #KEEP} cobblestone anywhere in the barrel. With
 * one it keeps {@value #PATTERN_COBBLESTONE} cobblestone in slot {@value #COBBLESTONE_SLOT} and
 * {@value #PATTERN_DIRT} dirt in slot {@value #DIRT_SLOT}.
 */
public class SupplierTestInstance extends GameTestInstance {

    public static final MapCodec<SupplierTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SupplierTestInstance::new));

    static final int KEEP = 32;
    static final int PATTERN_COBBLESTONE = 16;
    static final int PATTERN_DIRT = 8;
    static final int COBBLESTONE_SLOT = 5;
    static final int DIRT_SLOT = 2;
    /**
     * A supplier looks at its inventory every 100 ticks; this covers at least one more look.
     */
    private static final int SUPPLY_CYCLE = 120;

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos LINK = new BlockPos(3, 1, 1);
    private static final BlockPos SUPPLIER = new BlockPos(4, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(3, 1, 0);

    private final Scenario scenario;

    public SupplierTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(), scenario.stock.get());
        var suppliedBarrel = LPTestHelper.placeBarrel(helper, SUPPLIER.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        LPTestHelper.placePipe(helper, LINK, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var supplier = LPTestHelper.placePipe(helper, SUPPLIER, new PipeItemsSupplierLogistics(LPItems.PIPE_SUPPLIER.get()));
        BarrelBlockEntity overflowBarrel = null;
        if (scenario.overflow) {
            overflowBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.above());
            LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()))
                .getLogisticsModule().setDefaultRoute(true);
        }
        scenario.config.apply(supplier);

        var sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, supplier, provider);
                LPTestHelper.assertPowered(helper, supplier);
                LPTestHelper.assertPowered(helper, provider);
            });
        var steps = new Steps(helper, sequence, providerBarrel, suppliedBarrel, overflowBarrel);
        scenario.script.accept(steps);
        steps.sequence.thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Supplier: " + scenario.getSerializedName());
    }

    /**
     * How the supplier is set up: a plain request mode, or a pattern upgrade with its mode and
     * whether it is limited.
     */
    record Config(@Nullable SupplyMode supplyMode, @Nullable PatternMode patternMode, boolean limited) {

        static Config supply(SupplyMode mode) {
            return new Config(mode, null, true);
        }

        static Config pattern(PatternMode mode) {
            return new Config(null, mode, true);
        }

        static Config pattern(PatternMode mode, boolean limited) {
            return new Config(null, mode, limited);
        }

        void apply(PipeItemsSupplierLogistics supplier) {
            ModuleActiveSupplier module = supplier.getLogisticsModule();
            if (supplyMode != null) {
                module.requestMode.setValue(supplyMode);
                module.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, KEEP));
                return;
            }
            var patternUpgrade = BuiltInRegistries.ITEM.getValue(LPItems.upgrades.get(PatternUpgrade.getName()));
            var upgrades = ((UpgradeManager) supplier.getUpgradeManager()).inv;
            upgrades.setItem(0, new ItemStack(patternUpgrade));
            upgrades.setChanged();
            module.patternMode.setValue(patternMode);
            module.isLimited.setValue(limited);
            module.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, PATTERN_COBBLESTONE));
            module.slotAssignmentPattern.set(0, COBBLESTONE_SLOT);
            module.inventory.setItem(1, new ItemStack(Items.DIRT, PATTERN_DIRT));
            module.slotAssignmentPattern.set(1, DIRT_SLOT);
        }
    }

    /**
     * What a scenario does after the network is ready, step by step.
     */
    static final class Steps {

        private final GameTestHelper helper;
        private GameTestSequence sequence;
        private final BarrelBlockEntity provider;
        private final BarrelBlockEntity supplied;
        private final @Nullable BarrelBlockEntity overflow;

        Steps(GameTestHelper helper, GameTestSequence sequence, BarrelBlockEntity provider, BarrelBlockEntity supplied,
            @Nullable BarrelBlockEntity overflow) {
            this.helper = helper;
            this.sequence = sequence;
            this.provider = provider;
            this.supplied = supplied;
            this.overflow = overflow;
        }

        /**
         * Gives the supplier time for another look at its barrel, so a step that should not happen
         * had its chance.
         */
        Steps idleCycle() {
            sequence = sequence.thenIdle(SUPPLY_CYCLE);
            return this;
        }

        Steps waitSupplied(Item item, int count) {
            sequence = sequence.thenWaitUntil(() -> LPTestHelper.assertCount(helper, supplied, item, count, "Supplied barrel"));
            return this;
        }

        Steps waitSlot(int slot, Item item, int count) {
            sequence = sequence.thenWaitUntil(() -> assertSlot(slot, item, count));
            return this;
        }

        Steps expectSupplied(Item item, int count) {
            sequence = sequence.thenExecute(() -> LPTestHelper.assertCount(helper, supplied, item, count, "Supplied barrel"));
            return this;
        }

        Steps expectSlot(int slot, Item item, int count) {
            sequence = sequence.thenExecute(() -> assertSlot(slot, item, count));
            return this;
        }

        Steps expectProvider(Item item, int count) {
            sequence = sequence.thenExecute(() -> LPTestHelper.assertCount(helper, provider, item, count, "Provider"));
            return this;
        }

        Steps expectOverflow(Item item, int count) {
            sequence = sequence.thenExecute(() -> LPTestHelper.assertCount(helper,
                Objects.requireNonNull(overflow), item, count, "Default route barrel"));
            return this;
        }

        Steps waitOverflow(Item item, int count) {
            sequence = sequence.thenWaitUntil(() -> LPTestHelper.assertCount(helper,
                Objects.requireNonNull(overflow), item, count, "Default route barrel"));
            return this;
        }

        /**
         * Takes {@code count} of {@code item} out of the supplied barrel, from whichever slots hold it.
         */
        Steps take(Item item, int count) {
            sequence = sequence.thenExecute(() -> {
                int left = count;
                for (int i = 0; i < supplied.getContainerSize() && left > 0; i++) {
                    if (supplied.getItem(i).is(item)) {
                        left -= supplied.removeItem(i, left).getCount();
                    }
                }
                helper.assertTrue(left == 0, Component.literal("Could not take %d %s".formatted(count, item)));
            });
            return this;
        }

        Steps restock(Item item, int count) {
            sequence = sequence.thenExecute(() -> put(provider, new ItemStack(item, count)));
            return this;
        }

        Steps putInSlot(int slot, ItemStack stack) {
            sequence = sequence.thenExecute(() -> supplied.setItem(slot, stack));
            return this;
        }

        /**
         * Waits until the provider has sent some of its {@code item} on its way: the provider's
         * count has dropped below {@code stocked}, but nothing has reached the supplier yet.
         */
        Steps waitInTransit(Item item, int stocked) {
            sequence = sequence.thenWaitUntil(() -> helper.assertTrue(LPTestHelper.count(provider, item) < stocked,
                Component.literal("The provider has not sent any %s yet".formatted(item))));
            return this;
        }

        private void assertSlot(int slot, Item item, int count) {
            ItemStack stack = supplied.getItem(slot);
            int actual = stack.is(item) ? stack.getCount() : 0;
            helper.assertTrue(actual == count, Component.literal(
                "Slot %d should hold %d %s, holds %s".formatted(slot, count, item, stack)));
        }

        private static void put(Container container, ItemStack stack) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, stack);
                    return;
                }
            }
            throw new IllegalStateException("No room for " + stack);
        }
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Full: fills up, asks for nothing more while full, tops up what is taken out.
         */
        FULL_TOPS_UP(Config.supply(SupplyMode.Full), stock(64), s -> s
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, KEEP)
            .take(Items.COBBLESTONE, 10)
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, KEEP)
            .expectProvider(Items.COBBLESTONE, 64 - KEEP - 10)),
        /**
         * Full: asks for nothing until the network holds all that is missing.
         */
        FULL_WAITS_FOR_ALL(Config.supply(SupplyMode.Full), stock(20), s -> s
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, 0)
            .expectProvider(Items.COBBLESTONE, 20)
            .restock(Items.COBBLESTONE, KEEP - 20)
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .expectProvider(Items.COBBLESTONE, 0)),
        /**
         * Partial: takes what the network has and the rest once it is there.
         */
        PARTIAL_TAKES_WHAT_THERE_IS(Config.supply(SupplyMode.Partial), stock(20), s -> s
            .waitSupplied(Items.COBBLESTONE, 20)
            .expectProvider(Items.COBBLESTONE, 0)
            .restock(Items.COBBLESTONE, 64)
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, KEEP)
            .expectProvider(Items.COBBLESTONE, 64 - (KEEP - 20))),
        /**
         * Bulk 50%: tops up only once half or less is left.
         */
        BULK50_WAITS_FOR_HALF(Config.supply(SupplyMode.Bulk50), stock(64), s -> s
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .take(Items.COBBLESTONE, 10)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, KEEP - 10)
            .take(Items.COBBLESTONE, 10)
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .expectProvider(Items.COBBLESTONE, 64 - KEEP - 20)),
        /**
         * Bulk 100%: tops up only once nothing is left.
         */
        BULK100_WAITS_FOR_EMPTY(Config.supply(SupplyMode.Bulk100), stock(64), s -> s
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .take(Items.COBBLESTONE, KEEP - 1)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, 1)
            .take(Items.COBBLESTONE, 1)
            .waitSupplied(Items.COBBLESTONE, KEEP)
            .expectProvider(Items.COBBLESTONE, 0)),
        /**
         * Infinite: ignores the amount and takes everything there is room for.
         */
        INFINITE_TAKES_EVERYTHING(Config.supply(SupplyMode.Infinite), stock(64, 64), s -> s
            .waitSupplied(Items.COBBLESTONE, 128)
            .expectProvider(Items.COBBLESTONE, 0)),

        /**
         * Each item goes to the slot the pattern assigns it, and nowhere else.
         */
        PATTERN_PLACES_IN_SLOTS(Config.pattern(PatternMode.Full), () -> new ItemStack[] {new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIRT, 64)}, s -> s
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .waitSlot(DIRT_SLOT, Items.DIRT, PATTERN_DIRT)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .expectSupplied(Items.DIRT, PATTERN_DIRT)
            .expectSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .expectSlot(DIRT_SLOT, Items.DIRT, PATTERN_DIRT)),
        PATTERN_FULL_WAITS_FOR_ALL(Config.pattern(PatternMode.Full), stock(10), s -> s
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, 0)
            .expectProvider(Items.COBBLESTONE, 10)
            .restock(Items.COBBLESTONE, PATTERN_COBBLESTONE - 10)
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)),
        PATTERN_PARTIAL_TAKES_WHAT_THERE_IS(Config.pattern(PatternMode.Partial), stock(10), s -> s
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, 10)
            .restock(Items.COBBLESTONE, 64)
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, PATTERN_COBBLESTONE)),
        PATTERN_BULK50_WAITS_FOR_HALF(Config.pattern(PatternMode.Bulk50), stock(64), s -> s
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .take(Items.COBBLESTONE, 6)
            .idleCycle()
            .expectSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE - 6)
            .take(Items.COBBLESTONE, 3)
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .expectProvider(Items.COBBLESTONE, 64 - PATTERN_COBBLESTONE - 9)),
        PATTERN_BULK100_WAITS_FOR_EMPTY(Config.pattern(PatternMode.Bulk100), stock(64), s -> s
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .take(Items.COBBLESTONE, PATTERN_COBBLESTONE - 1)
            .idleCycle()
            .expectSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, 1)
            .take(Items.COBBLESTONE, 1)
            .waitSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .expectProvider(Items.COBBLESTONE, 64 - 2 * PATTERN_COBBLESTONE)),
        /**
         * A slot holding something else is left alone; nothing is asked for it.
         */
        PATTERN_SLOT_TAKEN(Config.pattern(PatternMode.Full), stock(64), s -> s
            .putInSlot(COBBLESTONE_SLOT, new ItemStack(Items.STONE, 1))
            .idleCycle()
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, 0)
            .expectProvider(Items.COBBLESTONE, 64)),
        /**
         * Limited: what does not fit in the slot when it arrives is sent back into the network.
         */
        PATTERN_LIMITED_SENDS_EXCESS_BACK(Config.pattern(PatternMode.Full, true), stock(64), true, s -> s
            .waitInTransit(Items.COBBLESTONE, 64)
            .putInSlot(COBBLESTONE_SLOT, new ItemStack(Items.COBBLESTONE, 10))
            .waitOverflow(Items.COBBLESTONE, 10)
            .expectSlot(COBBLESTONE_SLOT, Items.COBBLESTONE, PATTERN_COBBLESTONE)
            .expectSupplied(Items.COBBLESTONE, PATTERN_COBBLESTONE)),
        /**
         * Not limited: what does not fit in the slot goes in the way anything else would, here on
         * top of the stack already in that slot, and none goes back into the network.
         */
        PATTERN_UNLIMITED_KEEPS_EXCESS(Config.pattern(PatternMode.Full, false), stock(64), true, s -> s
            .waitInTransit(Items.COBBLESTONE, 64)
            .putInSlot(COBBLESTONE_SLOT, new ItemStack(Items.COBBLESTONE, 10))
            .waitSupplied(Items.COBBLESTONE, PATTERN_COBBLESTONE + 10)
            .idleCycle()
            .expectSupplied(Items.COBBLESTONE, PATTERN_COBBLESTONE + 10)
            .expectOverflow(Items.COBBLESTONE, 0));

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Config config;
        /**
         * Built when the test runs: stacks cannot be made while the enum loads, during registration.
         */
        final Supplier<ItemStack[]> stock;
        final boolean overflow;
        final Consumer<Steps> script;

        Scenario(Config config, Supplier<ItemStack[]> stock, Consumer<Steps> script) {
            this(config, stock, false, script);
        }

        Scenario(Config config, Supplier<ItemStack[]> stock, boolean overflow, Consumer<Steps> script) {
            this.config = config;
            this.stock = stock;
            this.overflow = overflow;
            this.script = script;
        }

        /**
         * The provider's stacks of cobblestone.
         */
        private static Supplier<ItemStack[]> stock(int... cobblestone) {
            return () -> {
                var stacks = new ItemStack[cobblestone.length];
                for (int i = 0; i < stacks.length; i++) {
                    stacks[i] = new ItemStack(Items.COBBLESTONE, cobblestone[i]);
                }
                return stacks;
            };
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
