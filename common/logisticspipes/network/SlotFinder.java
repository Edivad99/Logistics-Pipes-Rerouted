package logisticspipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.network.to_client.SlotFinderActivateMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.item.ItemIdentifier;

import network.rs485.logisticspipes.connection.LPNeighborTileEntityKt;

/**
 * The server half of "point at the slot you mean".
 *
 * <p>The supplier module can pin a pattern entry to one physical slot of a neighbouring
 * inventory, but neither side alone can name that slot: the client can only say which slot of the
 * open screen was clicked, and the screen belongs to the neighbour, not to the pipe. So the pipe's
 * GUI asks the server to open the neighbour, the server tells the client to highlight it, and the
 * click comes back here to be resolved against the real inventory.
 *
 * <p>This used to live inside the packets themselves. It is server logic, not serialization.
 */
public final class SlotFinder {

    private SlotFinder() {
    }

    /**
     * Opens the screen of an adjacent inventory that can take a pinned slot, and tells the player's
     * client to highlight it.
     */
    public static void openNeighbourInventory(Player player, ModuleTarget target, int slot) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final LogisticsTileGenericPipe pipe =
                TargetLookup.blockEntityAt(player, target.pos(), LogisticsTileGenericPipe.class);
        if (pipe == null || !pipe.isRoutingPipe()) {
            LogisticsPipes.LOG.warn("Ignored slot finder request from {}: no routing pipe at {}", player,
                    target.pos());
            return;
        }
        final boolean opened = pipe.getRoutingPipe().getAvailableAdjacent().inventories().stream()
                .filter(neighbor -> LPNeighborTileEntityKt.getInventoryUtil(neighbor) instanceof ISpecialInsertion)
                .anyMatch(neighbor -> {
                    for (ICraftingRecipeProvider provider : SimpleServiceLocator.craftingRecipeProviders) {
                        if (provider.canOpenGui(neighbor.getTileEntity())) {
                            return true;
                        }
                    }
                    final BlockPos pos = neighbor.getTileEntity().getBlockPos();
                    if (!openMenu(serverPlayer, pos)) {
                        return false;
                    }
                    PacketDistributor.sendToPlayer(serverPlayer, new SlotFinderActivateMessage(target, pos, slot));
                    return true;
                });
        if (!opened) {
            LogisticsPipes.LOG.warn("Ignored slot finder request from {}: no adjacent inventory to open", player);
        }
    }

    /**
     * Opens the block's own screen for the player, the way right-clicking it would.
     *
     * <p>Asking the block for its {@link MenuProvider} rather than replaying a right click is what
     * makes this safe to do on the player's behalf: no item is used, so nothing gets placed or
     * wrenched, and a block that refuses to open right now -- a chest with something sitting on top
     * -- says so by having no provider.
     */
    private static boolean openMenu(ServerPlayer player, BlockPos pos) {
        final BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        final MenuProvider provider = state.getMenuProvider(player.level(), pos);
        if (provider == null) {
            return false;
        }
        return player.openMenu(provider).isPresent();
    }

    /**
     * Resolves the slot the player clicked in the open screen to an index into the inventory
     * itself, and writes it into the module's pattern.
     */
    public static void assignSlot(Player player, ModuleTarget target, BlockPos inventoryPos, int menuSlotIndex,
            int slot) {
        final BlockEntity inventory = TargetLookup.blockEntityAt(player, inventoryPos, BlockEntity.class);
        if (inventory == null) {
            return;
        }
        final IInventoryUtil util = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inventory, null);
        if (util == null) {
            return;
        }
        final Slot clicked = menuSlot(player, menuSlotIndex);
        if (clicked == null) {
            player.sendSystemMessage(Component.translatable("lp.chat.slotnotfound"));
            return;
        }
        final int index = indexIn(util, clicked);
        if (index == -1) {
            player.sendSystemMessage(Component.translatable("lp.chat.slotnotfound"));
            return;
        }
        final ModuleActiveSupplier module = target.resolve(player, ModuleActiveSupplier.class);
        if (module != null) {
            module.slotAssignmentPattern.set(slot, index);
        }
    }

    /**
     * The clicked slot, by its position in the menu the player has open.
     *
     * <p>The client sends a menu position rather than a slot's own index because that is the only
     * handle both sides agree on: a slot's {@code index} is its position in whichever container it
     * belongs to, and a menu shows several containers at once.
     */
    private static @Nullable Slot menuSlot(Player player, int index) {
        final var slots = player.containerMenu.slots;
        return index >= 0 && index < slots.size() ? slots.get(index) : null;
    }

    /**
     * Which slot of the inventory the clicked screen slot is.
     *
     * <p>The comparison is by identity, because two slots holding equal stacks are not the same
     * slot. An empty slot has no stack to identify it, so it gets a unique one put in it for the
     * length of the search and emptied again afterwards.
     */
    private static int indexIn(IInventoryUtil util, Slot clicked) {
        final ItemStack content = clicked.getItem();
        if (!content.isEmpty()) {
            return indexOfIdentical(util, content);
        }
        final ItemStack marker = new ItemStack(Blocks.DIRT, 1);
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("LPStackFinderBoolean", true);
        marker.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        clicked.set(marker);
        try {
            final int identical = indexOfIdentical(util, marker);
            if (identical != -1) {
                return identical;
            }
            // Some inventories hand back copies rather than the stack they hold, so identity finds
            // nothing; the marker is unique enough that equality is safe as a fallback.
            for (int i = 0; i < util.getContainerSize(); i++) {
                final ItemStack stack = util.getItem(i);
                if (!stack.isEmpty() && ItemIdentifier.get(stack).equals(ItemIdentifier.get(marker))
                        && stack.getCount() == marker.getCount()) {
                    return i;
                }
            }
            return -1;
        } finally {
            clicked.set(ItemStack.EMPTY);
        }
    }

    private static int indexOfIdentical(IInventoryUtil util, ItemStack stack) {
        for (int i = 0; i < util.getContainerSize(); i++) {
            if (stack == util.getItem(i)) {
                return i;
            }
        }
        return -1;
    }
}
