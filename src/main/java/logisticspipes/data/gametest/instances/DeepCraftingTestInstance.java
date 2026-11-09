package logisticspipes.data.gametest.instances;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.MapCodec;

import logisticspipes.data.gametest.LPGameTestInstances;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

/**
 * A dispenser requested from a network that holds only raw materials, four crafters deep: logs
 * make planks, planks make sticks, sticks and string make the bow, and the bow goes into the
 * dispenser. A fifth crafter makes pistons out of the same planks.
 *
 * <p>Also meant to be run by hand, with {@code /test run logisticspipes:deep_crafting}: the setup
 * stays in place after the test, with stock left for more requests from the request table, such
 * as one to leave the world in the middle of.</p>
 */
public class DeepCraftingTestInstance extends GameTestInstance {

    public static final MapCodec<DeepCraftingTestInstance> CODEC =
        LPGameTestInstances.defaultCodec(DeepCraftingTestInstance::new);

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos PLANK_CRAFTER = new BlockPos(2, 1, 1);
    private static final BlockPos STICK_CRAFTER = new BlockPos(3, 1, 1);
    private static final BlockPos BOW_CRAFTER = new BlockPos(4, 1, 1);
    private static final BlockPos DISPENSER_CRAFTER = new BlockPos(5, 1, 1);
    private static final BlockPos PISTON_CRAFTER = new BlockPos(6, 1, 1);
    private static final BlockPos TABLE = new BlockPos(7, 1, 1);

    public DeepCraftingTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.STRING, 32),
            new ItemStack(Items.REDSTONE, 32), new ItemStack(Items.IRON_INGOT, 16));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));

        var plank = placeCrafter(helper, PLANK_CRAFTER, Items.OAK_LOG);
        var stick = placeCrafter(helper, STICK_CRAFTER,
            Items.OAK_PLANKS, null, null,
            Items.OAK_PLANKS);
        var bow = placeCrafter(helper, BOW_CRAFTER,
            null, Items.STICK, Items.STRING,
            Items.STICK, null, Items.STRING,
            null, Items.STICK, Items.STRING);
        var dispenser = placeCrafter(helper, DISPENSER_CRAFTER,
            Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE,
            Items.COBBLESTONE, Items.BOW, Items.COBBLESTONE,
            Items.COBBLESTONE, Items.REDSTONE, Items.COBBLESTONE);
        var piston = placeCrafter(helper, PISTON_CRAFTER,
            Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS,
            Items.COBBLESTONE, Items.IRON_INGOT, Items.COBBLESTONE,
            Items.COBBLESTONE, Items.REDSTONE, Items.COBBLESTONE);
        var table = LPTestHelper.placePipe(helper, TABLE, new PipeBlockRequestTable(LPItems.PIPE_REQUEST_TABLE.get()));
        var crafters = new PipeItemsCraftingLogistics[] { plank, stick, bow, dispenser, piston };

        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, table, provider);
                LPTestHelper.assertPowered(helper, table);
                LPTestHelper.assertPowered(helper, provider);
                for (var crafter : crafters) {
                    LPTestHelper.assertRouted(helper, table, crafter);
                    LPTestHelper.assertPowered(helper, crafter);
                }
            })
            .thenExecute(() -> {
                for (var crafter : crafters) {
                    crafter.getLogisticsModule().importFromCraftingTable(null);
                }
            })
            .thenWaitUntil(() -> {
                LPTestHelper.assertInterestedIn(helper, plank, Items.OAK_PLANKS);
                LPTestHelper.assertInterestedIn(helper, stick, Items.STICK);
                LPTestHelper.assertInterestedIn(helper, bow, Items.BOW);
                LPTestHelper.assertInterestedIn(helper, dispenser, Items.DISPENSER);
                LPTestHelper.assertInterestedIn(helper, piston, Items.PISTON);
            })
            .thenExecute(() -> helper.assertTrue(LPTestHelper.request(table, new ItemStack(Items.DISPENSER)),
                Component.literal("The request for a dispenser should have been accepted")))
            .thenWaitUntil(() -> LPTestHelper.assertCount(helper, table.inv, Items.DISPENSER, 1, "Table"))
            .thenExecute(() -> {
                // One log made four planks, two of which made four sticks: two planks and one stick
                // are left over, back in stock or still with the crafters.
                LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_LOG, 31, "Provider");
                LPTestHelper.assertCount(helper, providerBarrel, Items.STRING, 29, "Provider");
                LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE, 57, "Provider");
            })
            .thenSucceed();
    }

    /**
     * Places a crafting pipe with a crafting table south of it, laid out with {@code matrix} from
     * the top left, row by row; a null leaves a slot empty.
     */
    private static PipeItemsCraftingLogistics placeCrafter(GameTestHelper helper, BlockPos pos, Item... matrix) {
        helper.setBlock(pos.south(), LPBlocks.CRAFTER.get());
        var table = helper.getBlockEntity(pos.south(), LogisticsCraftingTableBlockEntity.class);
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null) {
                table.matrix.setItem(i, new ItemStack(matrix[i]));
            }
        }
        table.cacheRecipe();
        return LPTestHelper.placePipe(helper, pos, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Deep Crafting");
    }
}
