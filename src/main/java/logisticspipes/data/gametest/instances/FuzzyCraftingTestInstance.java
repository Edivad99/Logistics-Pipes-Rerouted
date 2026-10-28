package logisticspipes.data.gametest.instances;

import java.util.Locale;
import java.util.function.BiConsumer;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsUnrouted;
import logisticspipes.pipes.upgrades.FuzzyUpgrade;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.utils.FuzzyFlag;
import logisticspipes.utils.FuzzyUtil;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsGenericPipeBlock;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

/**
 * A chest crafted by a crafting pipe on a fuzzy crafting table, from oak and dark oak planks.
 *
 * <p>When the recipe matches planks by their tag, one craft may take planks of both kinds, as a
 * vanilla crafting table would, whether the eight planks are one ingredient (imported from the
 * table) or eight (one in each slot of the crafting pipe). When it matches them exactly, it takes
 * oak planks only.
 *
 * <p>The scenarios ending in {@code after_reload} save and load the crafting pipe back once it is
 * set up, so its recipe, its fuzzy flags and its upgrade must survive that.
 */
public class FuzzyCraftingTestInstance extends GameTestInstance {

    public static final MapCodec<FuzzyCraftingTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FuzzyCraftingTestInstance::new));

    private static final BlockPos TABLE = new BlockPos(3, 1, 4);
    private static final BlockPos CRAFTER = new BlockPos(3, 1, 3);
    private static final BlockPos POWER_PIPE = new BlockPos(4, 1, 3);
    private static final BlockPos POWER_JUNCTION = new BlockPos(5, 1, 3);
    private static final BlockPos PROVIDER = new BlockPos(2, 1, 3);
    private static final BlockPos PROVIDER_CHEST = new BlockPos(1, 1, 3);
    private static final BlockPos REQUESTER = new BlockPos(3, 2, 3);
    private static final BlockPos REQUESTER_CHEST = new BlockPos(3, 3, 3);

    private final Scenario scenario;

    public FuzzyCraftingTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        helper.setBlock(TABLE, LPBlocks.CRAFTER_FUZZY.get());
        var table = helper.getBlockEntity(TABLE, LogisticsCraftingTableBlockEntity.class);
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                table.matrix.setItem(i, new ItemStack(Items.OAK_PLANKS));
                FuzzyUtil.set(table.inputFuzzy(i), FuzzyFlag.USE_ORE_DICT, scenario.ingredient != Ingredient.EXACT);
            }
        }
        table.cacheRecipe();

        LPTestHelper.placePowerJunction(helper, POWER_JUNCTION);
        var providerChest = LPTestHelper.placeChest(helper, PROVIDER_CHEST, scenario.providerStacks());
        var requesterChest = LPTestHelper.placeChest(helper, REQUESTER_CHEST);
        var crafter = LPTestHelper.placePipe(helper, CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        var fuzzyUpgrade = BuiltInRegistries.ITEM.getValue(LPItems.upgrades.get(FuzzyUpgrade.getName()));
        var upgrades = ((UpgradeManager) crafter.getUpgradeManager()).inv;
        upgrades.setItem(0, new ItemStack(fuzzyUpgrade));
        upgrades.setChanged();
        LPTestHelper.placePipe(helper, POWER_PIPE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));
        var secondProvider = scenario.secondProvider.place(helper);

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, crafter);
                LPTestHelper.assertRouted(helper, requester, provider);
                LPTestHelper.assertRouted(helper, crafter, provider);
                if (secondProvider != null) {
                    LPTestHelper.assertRouted(helper, crafter, secondProvider);
                }
                LPTestHelper.assertPowered(helper, crafter);
                LPTestHelper.assertPowered(helper, provider);
            })
            .thenExecute(() -> {
                crafter.getLogisticsModule().importFromCraftingTable(null);
                helper.assertTrue(crafter.getLogisticsModule().hasFuzzyUpgrade(),
                    Component.literal("The crafting pipe should have its fuzzy upgrade"));
                if (scenario.ingredient == Ingredient.SPLIT) {
                    for (int i = 0; i < 8; i++) {
                        crafter.getDummyInventory().setItem(i, new ItemStack(Items.OAK_PLANKS));
                        FuzzyUtil.set(crafter.getLogisticsModule().inputFuzzy(i), FuzzyFlag.USE_ORE_DICT, true);
                    }
                }
            })
            .thenExecute(() -> {
                if (scenario.reloadCrafter) {
                    LPTestHelper.reload(helper, CRAFTER);
                }
            })
            // A refused request only means something once a request can find the crafter at all
            .thenWaitUntil(() -> {
                var current = LPTestHelper.pipeAt(helper, CRAFTER, PipeItemsCraftingLogistics.class);
                LPTestHelper.assertRouted(helper, requester, current);
                LPTestHelper.assertInterestedIn(helper, current, Items.CHEST);
            })
            .thenExecute(() -> {
                boolean accepted = scenario.multiRequest
                    ? LPTestHelper.requestAll(requester, new ItemStack(Items.CHEST))
                    : LPTestHelper.request(requester, new ItemStack(Items.CHEST));
                helper.assertTrue(accepted == scenario.mustSucceed, Component.literal(scenario.mustSucceed
                    ? "The request for a chest should have been accepted"
                    : "The request for a chest should have been refused"));
            });

        if (scenario.mustSucceed) {
            sequence
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, requesterChest, Items.CHEST, 1, "Requester"))
                .thenExecute(() -> {
                    LPTestHelper.assertOnly(helper, requesterChest, Items.CHEST, "Requester");
                    scenario.providerCheck.accept(helper, providerChest);
                })
                .thenSucceed();
        } else {
            sequence
                .thenIdle(60)
                .thenExecute(() -> {
                    LPTestHelper.assertCount(helper, requesterChest, Items.CHEST, 0, "Requester");
                    scenario.providerCheck.accept(helper, providerChest);
                })
                .thenSucceed();
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Fuzzy Crafting: " + scenario.getSerializedName());
    }

    /**
     * Where a second chest of dark oak planks sits, if the scenario has one.
     */
    enum SecondProvider {
        NONE,
        /**
         * On a provider pipe of its own, its chest right next to the first one.
         */
        ADJACENT,
        /**
         * Behind an unrouted pipe, so it is reached over a different path.
         */
        BEHIND_UNROUTED;

        @Nullable PipeItemsProviderLogistics place(GameTestHelper helper) {
            if (this == NONE) {
                return null;
            }
            BlockPos link = new BlockPos(3, 1, 2);
            if (this == BEHIND_UNROUTED) {
                if (!LogisticsGenericPipeBlock.placePipe(new PipeItemsUnrouted(LPItems.PIPE_UNROUTED.get()),
                    helper.getLevel(), helper.absolutePos(link), LPBlocks.PIPE.get())) {
                    throw helper.assertionException(link, "Could not place the unrouted pipe");
                }
                link = link.north();
            }
            LPTestHelper.placeChest(helper, link.west(2), new ItemStack(Items.DARK_OAK_PLANKS, 4));
            LPTestHelper.placePipe(helper, link, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
            return LPTestHelper.placePipe(helper, link.west(), new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        }
    }

    /**
     * How the crafting pipe sees the eight planks of the recipe.
     */
    enum Ingredient {
        /**
         * One ingredient of eight planks matched by tag, as imported from the table.
         */
        SINGLE,
        /**
         * Eight ingredients of one plank each, every one matched by tag.
         */
        SPLIT,
        /**
         * One ingredient of eight oak planks, matched exactly.
         */
        EXACT
    }

    public enum Scenario implements StringRepresentable {
        SINGLE_MIXED_SUCCEEDS(Ingredient.SINGLE, false, true, SecondProvider.NONE, 4, 4, Scenario::empty),
        SINGLE_MIXED_SUCCEEDS_MULTI(Ingredient.SINGLE, true, true, SecondProvider.NONE, 4, 4, Scenario::empty),
        SINGLE_LEFTOVER_SUCCEEDS(Ingredient.SINGLE, false, true, SecondProvider.NONE, 4, 8, Scenario::fourPlanksLeft),
        SINGLE_LEFTOVER_SUCCEEDS_MULTI(Ingredient.SINGLE, true, true, SecondProvider.NONE, 4, 8, Scenario::fourPlanksLeft),
        SINGLE_MIXED_ADJACENT_PROVIDERS_SUCCEEDS(Ingredient.SINGLE, false, true, SecondProvider.ADJACENT, 4, 0, Scenario::empty),
        SINGLE_MIXED_DIFFERENT_PROVIDERS_SUCCEEDS(Ingredient.SINGLE, false, true, SecondProvider.BEHIND_UNROUTED, 4, 0, Scenario::empty),
        SPLIT_MIXED_SUCCEEDS(Ingredient.SPLIT, false, true, SecondProvider.NONE, 4, 4, Scenario::empty),
        SPLIT_MIXED_SUCCEEDS_MULTI(Ingredient.SPLIT, true, true, SecondProvider.NONE, 4, 4, Scenario::empty),
        SPLIT_LEFTOVER_SUCCEEDS(Ingredient.SPLIT, false, true, SecondProvider.NONE, 4, 8, Scenario::fourPlanksLeft),
        SPLIT_LEFTOVER_SUCCEEDS_MULTI(Ingredient.SPLIT, true, true, SecondProvider.NONE, 4, 8, Scenario::fourPlanksLeft),
        SINGLE_MIXED_SUCCEEDS_AFTER_RELOAD(Ingredient.SINGLE, false, true, SecondProvider.NONE, 4, 4, Scenario::empty, true),
        SPLIT_MIXED_SUCCEEDS_AFTER_RELOAD(Ingredient.SPLIT, false, true, SecondProvider.NONE, 4, 4, Scenario::empty, true),
        EXACT_MIXED_FAILS_AFTER_RELOAD(Ingredient.EXACT, false, false, SecondProvider.NONE, 4, 8, Scenario::untouched, true),
        EXACT_ONE_KIND_SUCCEEDS(Ingredient.EXACT, false, true, SecondProvider.NONE, 8, 8, Scenario::onlyDarkOakLeft),
        EXACT_MIXED_FAILS(Ingredient.EXACT, false, false, SecondProvider.NONE, 4, 8, Scenario::untouched);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Ingredient ingredient;
        final boolean multiRequest;
        final boolean mustSucceed;
        final SecondProvider secondProvider;
        private final int oakPlanks;
        private final int darkOakPlanks;
        /**
         * What the first provider's chest should hold once the test is over.
         */
        final BiConsumer<GameTestHelper, Container> providerCheck;
        final boolean reloadCrafter;

        Scenario(Ingredient ingredient, boolean multiRequest, boolean mustSucceed, SecondProvider secondProvider,
            int oakPlanks, int darkOakPlanks, BiConsumer<GameTestHelper, Container> providerCheck) {
            this(ingredient, multiRequest, mustSucceed, secondProvider, oakPlanks, darkOakPlanks, providerCheck, false);
        }

        Scenario(Ingredient ingredient, boolean multiRequest, boolean mustSucceed, SecondProvider secondProvider,
            int oakPlanks, int darkOakPlanks, BiConsumer<GameTestHelper, Container> providerCheck,
            boolean reloadCrafter) {
            this.ingredient = ingredient;
            this.multiRequest = multiRequest;
            this.mustSucceed = mustSucceed;
            this.secondProvider = secondProvider;
            this.oakPlanks = oakPlanks;
            this.darkOakPlanks = darkOakPlanks;
            this.providerCheck = providerCheck;
            this.reloadCrafter = reloadCrafter;
        }

        ItemStack[] providerStacks() {
            if (darkOakPlanks == 0) {
                return new ItemStack[] {new ItemStack(Items.OAK_PLANKS, oakPlanks)};
            }
            return new ItemStack[] {
                new ItemStack(Items.OAK_PLANKS, oakPlanks),
                new ItemStack(Items.DARK_OAK_PLANKS, darkOakPlanks)};
        }

        private static void empty(GameTestHelper helper, Container chest) {
            helper.assertTrue(chest.isEmpty(), Component.literal("The provider should have given all its planks"));
        }

        private static void fourPlanksLeft(GameTestHelper helper, Container chest) {
            int left = LPTestHelper.count(chest, Items.OAK_PLANKS) + LPTestHelper.count(chest, Items.DARK_OAK_PLANKS);
            helper.assertTrue(left == 4, Component.literal("The provider should still hold 4 planks, holds " + left));
        }

        /**
         * An exact recipe has no use for the dark oak.
         */
        private static void onlyDarkOakLeft(GameTestHelper helper, Container chest) {
            LPTestHelper.assertCount(helper, chest, Items.OAK_PLANKS, 0, "Provider");
            LPTestHelper.assertCount(helper, chest, Items.DARK_OAK_PLANKS, 8, "Provider");
        }

        private static void untouched(GameTestHelper helper, Container chest) {
            LPTestHelper.assertCount(helper, chest, Items.OAK_PLANKS, 4, "Provider");
            LPTestHelper.assertCount(helper, chest, Items.DARK_OAK_PLANKS, 8, "Provider");
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
