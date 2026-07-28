package logisticspipes.world.inventory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.utils.gui.ColorSlot;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;
import logisticspipes.utils.gui.HandelableSlot;
import logisticspipes.utils.gui.ModuleSlot;
import logisticspipes.utils.gui.RestrictedSlot;
import logisticspipes.utils.gui.UnmodifiableSlot;

public abstract class DummyMenu extends AbstractContainerMenu {

    private final List<Slot> transferTop = new ArrayList<>();
    private final List<Slot> transferBottom = new ArrayList<>();
    private final List<BitSet> slotsFuzzyFlags = new ArrayList<>();

    @Getter
    private final Player player;
    private final BlockEntity blockEntity;

    protected DummyMenu(@Nullable MenuType<?> menuType, int containerId, Player player, BlockEntity blockEntity) {
        super(menuType, containerId);
        this.player = player;
        this.blockEntity = blockEntity;
        if (this.player instanceof ServerPlayer) {
            if (blockEntity instanceof IScreenOpenController openController) {
                openController.screenOpenedByPlayer(player);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        if (transferTop.isEmpty() || transferBottom.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(i);
        if (slot instanceof DummySlot || slot instanceof UnmodifiableSlot || slot instanceof FluidSlot
            || slot instanceof ColorSlot || slot instanceof HandelableSlot || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (transferTop.contains(slot)) {
            handleShiftClickLists(slot, transferBottom, true, player);
            handleShiftClickLists(slot, transferBottom, false, player);
        } else if (transferBottom.contains(slot)) {
            handleShiftClickLists(slot, transferTop, true, player);
            handleShiftClickLists(slot, transferTop, false, player);
        }
        return ItemStack.EMPTY;
    }

    private void handleShiftClickLists(Slot from, List<Slot> toList, boolean ignoreEmpty, Player player) {
        if (!from.hasItem()) {
            return;
        }
        for (Slot to : toList) {
            if (handleShiftClickForSlots(from, to, ignoreEmpty, player)) {
                return;
            }
        }
    }

    private boolean handleShiftClickForSlots(Slot from, Slot to, boolean ignoreEmpty, Player player) {
        if (!from.hasItem()) {
            return true;
        }
        ItemStack out = from.getItem();
        if (!to.hasItem() && !ignoreEmpty && to.mayPlace(out)) {
            boolean remove = true;
            if (out.getCount() > to.getMaxStackSize()) {
                out = from.remove(to.getMaxStackSize());
                remove = false;
            }
            from.onTake(player, out);
            to.set(out);
            if (remove) {
                from.set(ItemStack.EMPTY);
            }
            return true;
        }
        if (from instanceof ModuleSlot || to instanceof ModuleSlot) {
            return false;
        }
        from.onTake(player, out);
        if (to.hasItem() && ItemStack.isSameItem(to.getItem(), out) && ItemStack.isSameItemSameComponents(to.getItem(),
            from.getItem())) {
            int free = Math.min(to.getMaxStackSize(), to.getItem().getMaxStackSize()) - to.getItem().getCount();
            if (free > 0) {
                ItemStack toInsert = from.remove(free);
                from.onTake(player, toInsert);
                ItemStack toStack = to.getItem();
                if (!toInsert.isEmpty() && !toStack.isEmpty()) {
                    toStack.grow(toInsert.getCount());
                    to.set(toStack);
                    return !from.hasItem();
                }
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer) {
            if (blockEntity instanceof IScreenOpenController openController) {
                openController.screenClosedByPlayer(player);
            }
        }
        super.removed(player);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        this.slotsFuzzyFlags.add(null);
        return super.addSlot(slot);
    }

    protected Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, Item item) {
        return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, item));
    }

    protected void addNormalSlotsForPlayerInventory(Inventory inventory, int xOffset, int yOffset) {
        // Player "backpack"
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                Slot slot = new Slot(inventory, column + row * 9 + 9, xOffset + column * 18, yOffset + row * 18);
                addSlot(slot);
                transferBottom.add(slot);
            }
        }

        // Player "hotbar"
        for (int row = 0; row < 9; row++) {
            Slot slot = new Slot(inventory, row, xOffset + row * 18, yOffset + 58);
            addSlot(slot);
            transferBottom.add(slot);
        }
    }
}
