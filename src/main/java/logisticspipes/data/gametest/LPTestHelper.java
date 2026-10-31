package logisticspipes.data.gametest;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.request.RequestTree;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.LogisticsGenericPipeBlock;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

/**
 * Builds logistics networks inside a game test and checks what they did. Positions are relative to
 * the test structure, like every other {@link GameTestHelper} call.
 */
public final class LPTestHelper {

    private LPTestHelper() {
    }

    public static <T extends CoreRoutedPipe> T placePipe(GameTestHelper helper, BlockPos pos, T pipe) {
        if (!LogisticsGenericPipeBlock.placePipe(pipe, helper.getLevel(), helper.absolutePos(pos),
            LPBlocks.PIPE.get())) {
            throw helper.assertionException(pos, "Could not place %s", pipe);
        }
        return pipe;
    }

    /**
     * The pipe at {@code pos} now. Look it up again after a {@link #reload}: the old object is gone.
     */
    public static <T extends CoreRoutedPipe> T pipeAt(GameTestHelper helper, BlockPos pos, Class<T> type) {
        var pipe = helper.getBlockEntity(pos, LogisticsGenericPipeBlockEntity.class).pipe;
        if (!type.isInstance(pipe)) {
            throw helper.assertionException(pos, "Expected a %s, found %s", type.getSimpleName(), pipe);
        }
        return type.cast(pipe);
    }

    /**
     * Puts a fresh block entity in place of the one at {@code pos}, loaded from what the old one
     * saved, the way a chunk going out of view and coming back would.
     */
    public static void reload(GameTestHelper helper, BlockPos pos) {
        var level = helper.getLevel();
        var absolute = helper.absolutePos(pos);
        var registries = level.registryAccess();
        var old = Objects.requireNonNull(level.getBlockEntity(absolute), () -> "No block entity at " + pos);
        var saved = old.saveWithFullMetadata(registries);
        old.onChunkUnloaded();
        level.removeBlockEntity(absolute);
        var fresh = BlockEntity.loadStatic(absolute, level.getBlockState(absolute), saved, registries);
        if (fresh == null) {
            throw helper.assertionException(pos, "Could not load the block entity back");
        }
        level.setBlockEntity(fresh);
    }

    /**
     * Places a chassis pipe with {@code module} in its first slot and returns that module. The
     * module goes in through the chassis's module inventory, as a player's click would put it.
     */
    public static <M extends LogisticsModule> M placeChassis(GameTestHelper helper, BlockPos pos,
        String moduleName, Class<M> moduleType) {
        var chassis = placePipe(helper, pos,
            new PipeLogisticsChassis(LPItems.PIPE_CHASSIS_MK1.get(), PipeLogisticsChassis.Tier.MK1));
        var moduleItem = BuiltInRegistries.ITEM.getValue(LPItems.modules.get(moduleName));
        var modules = chassis.getModuleInventory(helper.getLevel().registryAccess());
        modules.setItem(0, new ItemStack(moduleItem));
        modules.setChanged();
        if (!moduleType.isInstance(chassis.getSubModule(0))) {
            throw helper.assertionException(pos, "The chassis did not install a %s", moduleType.getSimpleName());
        }
        return moduleType.cast(chassis.getSubModule(0));
    }

    /**
     * Puts {@code upgrade} in the first free upgrade slot of {@code pipe}.
     */
    public static void addUpgrade(CoreRoutedPipe pipe, String upgradeName) {
        addUpgrade(pipe, upgradeStack(upgradeName));
    }

    /**
     * Puts {@code upgrade} in the first free upgrade slot of {@code pipe}, as it is: for an upgrade
     * that carries its configuration on the stack.
     */
    public static void addUpgrade(CoreRoutedPipe pipe, ItemStack upgrade) {
        var upgrades = ((UpgradeManager) pipe.getUpgradeManager()).inv;
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            if (upgrades.getItem(i).isEmpty()) {
                upgrades.setItem(i, upgrade);
                upgrades.setChanged();
                return;
            }
        }
        throw new IllegalStateException("No free upgrade slot on " + describe(pipe));
    }

    public static ItemStack upgradeStack(String upgradeName) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(LPItems.upgrades.get(upgradeName)));
    }

    /**
     * A power junction charged well beyond what any test spends, so a test only fails on power when
     * power is what it is about.
     */
    public static void placePowerJunction(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, LPBlocks.POWER_JUNCTION.get());
        helper.getBlockEntity(pos, LogisticsPowerJunctionBlockEntity.class).addEnergy(1_000_000);
    }

    /**
     * Barrels, not chests: two chests next to each other merge into one double chest.
     */
    public static BarrelBlockEntity placeBarrel(GameTestHelper helper, BlockPos pos, ItemStack... stacks) {
        helper.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity barrel = helper.getBlockEntity(pos, BarrelBlockEntity.class);
        for (int i = 0; i < stacks.length; i++) {
            barrel.setItem(i, stacks[i]);
        }
        return barrel;
    }

    public static ChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos, ItemStack... stacks) {
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        for (int i = 0; i < stacks.length; i++) {
            chest.setItem(i, stacks[i]);
        }
        return chest;
    }

    /**
     * Fails until {@code from} has finished its first tick and has a route to {@code to}; meant for
     * {@code thenWaitUntil}, since the routing tables are built off the server thread.
     */
    public static void assertRouted(GameTestHelper helper, CoreRoutedPipe from, CoreRoutedPipe to) {
        if (from.initialInit() || to.initialInit() || from.getRouter().getDistanceTo(to.getRouter()).isEmpty()) {
            throw helper.assertionException("%s has no route to %s yet", describe(from), describe(to));
        }
    }

    /**
     * Fails until {@code pipe} can draw power. A pipe finds its power junction through its own
     * routing table, so being routed to is not enough: a sink without power turns items away.
     */
    public static void assertPowered(GameTestHelper helper, CoreRoutedPipe pipe) {
        if (!pipe.canUseEnergy(1)) {
            throw helper.assertionException("%s has no power yet", describe(pipe));
        }
    }

    /**
     * Fails until {@code pipe} has declared an interest in {@code item}. Requests only look for
     * providers and crafters among the pipes interested in what they want, and a pipe refreshes its
     * interests every {@link ServerRouter#REFRESH_TIME} ticks, so a pipe configured a moment ago is
     * not found yet.
     */
    public static void assertInterestedIn(GameTestHelper helper, CoreRoutedPipe pipe, Item item) {
        assertInterestedIn(helper, pipe, ItemIdentifier.get(item));
    }

    /**
     * As {@link #assertInterestedIn(GameTestHelper, CoreRoutedPipe, Item)}, for an item that is
     * more than its type: a fluid, say, which the network knows by its container's identifier.
     */
    public static void assertInterestedIn(GameTestHelper helper, CoreRoutedPipe pipe, ItemIdentifier item) {
        if (!ServerRouter.getRoutersInterestedIn(item).get(pipe.getRouter().getSimpleID())) {
            throw helper.assertionException("%s is not interested in %s yet", describe(pipe), item);
        }
    }

    public static void assertNotRouted(GameTestHelper helper, CoreRoutedPipe from, CoreRoutedPipe to) {
        if (!from.getRouter().getDistanceTo(to.getRouter()).isEmpty()) {
            throw helper.assertionException("%s still has a route to %s", describe(from), describe(to));
        }
    }

    public static <T extends CoreRoutedPipe & IRequestItems> boolean request(T requester, ItemStack stack) {
        return RequestTree.request(ItemIdentifierStack.getFromStack(stack), requester, null, null);
    }

    /**
     * Asks for as much of {@code stack} as the network can give, and returns how much that is.
     */
    public static <T extends CoreRoutedPipe & IRequestItems> int requestPartial(T requester, ItemStack stack) {
        return RequestTree.requestPartial(ItemIdentifierStack.getFromStack(stack), requester, null);
    }

    /**
     * The same as {@link #request} for a single stack, but through the path a request for a whole
     * list of items takes.
     */
    public static <T extends CoreRoutedPipe & IRequestItems> boolean requestAll(T requester, ItemStack... stacks) {
        return RequestTree.request(Arrays.stream(stacks).map(ItemIdentifierStack::getFromStack).toList(), requester,
            null, RequestTree.defaultRequestFlags, null);
    }

    public static int count(Container container, Item item) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void assertCount(GameTestHelper helper, Container container, Item item, int expected,
        String where) {
        int actual = count(container, item);
        helper.assertTrue(actual == expected, Component.literal(
            "%s should hold %d %s, holds %d".formatted(where, expected, item, actual)));
    }

    public static void assertOnly(GameTestHelper helper, Container container, Item item, String where) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && !stack.is(item)) {
                throw helper.assertionException("%s should hold only %s, also holds %s", where, item, stack);
            }
        }
    }

    private static String describe(CoreRoutedPipe pipe) {
        return pipe.getClass().getSimpleName() + " at " + pipe.getPos();
    }
}
