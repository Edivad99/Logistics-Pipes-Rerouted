package logisticspipes.world.inventory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.PacketDistributor;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.interfaces.ISlotCheck;
import logisticspipes.interfaces.ISlotClick;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.network.bidirectional.FuzzySlotFlagsMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.MinecraftColor;
import logisticspipes.utils.gui.ColorSlot;
import logisticspipes.utils.gui.DummySlot;
import logisticspipes.utils.gui.FluidSlot;
import logisticspipes.utils.gui.FuzzyDummySlot;
import logisticspipes.utils.gui.FuzzyUnmodifiableSlot;
import logisticspipes.utils.gui.HandelableSlot;
import logisticspipes.utils.gui.IJeiScreenHolder;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.utils.gui.ModuleSlot;
import logisticspipes.utils.gui.RestrictedSlot;
import logisticspipes.utils.gui.SneakyUpgradeSlot;
import logisticspipes.utils.gui.StaticRestrictedSlot;
import logisticspipes.utils.gui.UpgradeSlot;
import logisticspipes.utils.gui.UnmodifiableSlot;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.property.IBitSet;

public abstract class DummyMenu extends AbstractContainerMenu implements IJeiScreenHolder {

    private @Nullable LogisticsBaseGuiScreen screenForJEI;

    private final List<Slot> transferTop = new ArrayList<>();
    private final List<Slot> transferBottom = new ArrayList<>();
    private final List<BitSet> slotsFuzzyFlags = new ArrayList<>();

    /** Recipe viewers step a ghost slot's count with these instead of a real mouse button. */
    private static final int STEP_UP = 1000;
    private static final int STEP_DOWN = 1001;

    /** Vanilla keeps its own listener list private, so the fuzzy sync tracks them here. */
    private final List<ContainerListener> listeners = new ArrayList<>();

    private long lastClicked;
    private long lastDragLookup;
    private boolean draggingGhostSlots;

    @Getter
    private final Player player;
    private final BlockEntity blockEntity;

    protected DummyMenu(@Nullable MenuType<?> menuType, int containerId, Player player,
        @Nullable BlockEntity blockEntity) {
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
    public @Nullable LogisticsBaseGuiScreen getScreenForJEI() {
        return screenForJEI;
    }

    @Override
    public void setScreenForJEI(LogisticsBaseGuiScreen screen) {
        screenForJEI = screen;
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

    /**
     * Ghost slots do not move items, they are edited.
     *
     * <p>Vanilla's click handling would try to pick the stack up; for a slot that only shows what a
     * filter is set to, the click means "set me to what the cursor holds", "clear me", or "step my
     * count". Anything that is a real slot falls through to vanilla untouched.
     */
    @Override
    public void clicked(int slotId, int mouseButton, ContainerInput input, Player player) {
        lastClicked = System.currentTimeMillis();
        if (slotId < 0 || slotId >= slots.size()) {
            super.clicked(slotId, mouseButton, input, player);
            return;
        }
        final Slot slot = slots.get(slotId);
        if (!isGhostSlot(slot)) {
            super.clicked(slotId, mouseButton, input, player);
            return;
        }
        final ItemStack carried = getCarried();
        // A double click with nothing on the cursor arrives as a click and a PICKUP_ALL; the
        // second would undo the first.
        if (carried.isEmpty() && input == ContainerInput.PICKUP_ALL) {
            return;
        }
        if (slot instanceof HandelableSlot handelable) {
            if (carried.isEmpty()) {
                setCarried(handelable.getProvidedStack());
            }
            return;
        }
        if (slot instanceof UnmodifiableSlot) {
            return;
        }
        editGhostSlot(slot, slotId, carried, mouseButton, input, player);
    }

    private static boolean isGhostSlot(Slot slot) {
        return slot instanceof DummySlot || slot instanceof UnmodifiableSlot || slot instanceof FluidSlot
            || slot instanceof ColorSlot || slot instanceof HandelableSlot;
    }

    private void editGhostSlot(Slot slot, int slotId, ItemStack carried, int mouseButton, ContainerInput input,
        Player player) {
        if (slot instanceof FluidSlot) {
            editFluidSlot(slot, slotId, carried, mouseButton, player);
            return;
        }
        if (slot instanceof ColorSlot) {
            editColorSlot(slot, slotId, carried, mouseButton, player);
            return;
        }
        if (slot instanceof DummySlot dummy) {
            dummy.setRedirectCall(true);
        }
        try {
            editItemGhostSlot(slot, carried, mouseButton, input);
        } finally {
            if (slot instanceof DummySlot dummy) {
                dummy.setRedirectCall(false);
            }
        }
    }

    /**
     * A fluid cannot be carried on the cursor, so an empty slot asks the client which fluid to
     * filter for; a bucket or tank on the cursor names one directly.
     */
    private void editFluidSlot(Slot slot, int slotId, ItemStack carried, int mouseButton, Player player) {
        if (!carried.isEmpty()) {
            final FluidIdentifier carriedFluid = FluidIdentifier.get(carried);
            if (carriedFluid != null) {
                slot.set(mouseButton == 0 ? carriedFluid.getItemIdentifier().makeNormalStack(1) : ItemStack.EMPTY);
                return;
            }
        }
        final FluidIdentifier current = slot.getItem().isEmpty() ? null
            : FluidIdentifier.get(ItemIdentifier.get(slot.getItem()));
        if (current == null && player instanceof LocalPlayer) {
            MainProxy.getProxy(true).openFluidSelectGui(slotId);
        }
        slot.set(ItemStack.EMPTY);
    }

    private void editColorSlot(Slot slot, int slotId, ItemStack carried, int mouseButton, Player player) {
        final MinecraftColor carriedColor = MinecraftColor.getColor(carried);
        if (MinecraftColor.BLANK.equals(carriedColor)) {
            MinecraftColor color = MinecraftColor.getColor(slot.getItem());
            if (mouseButton == 0) {
                color = color.getNext();
            } else if (mouseButton == 1) {
                color = color.getPrev();
            } else {
                color = MinecraftColor.BLANK;
            }
            slot.set(color.getItemStack());
        } else {
            slot.set(mouseButton == 1 ? MinecraftColor.BLANK.getItemStack() : carriedColor.getItemStack());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), slotId, slot.getItem()));
        }
    }

    private void editItemGhostSlot(Slot slot, ItemStack carried, int mouseButton, ContainerInput input) {
        // The recipe-viewer buttons that step a filter's count come in as these two.
        if (mouseButton == STEP_UP || mouseButton == STEP_DOWN) {
            if (slot.hasItem()) {
                final ItemStack stack = slot.getItem().copy();
                if (mouseButton == STEP_UP) {
                    stack.grow(1);
                } else if (stack.getCount() > 1) {
                    stack.shrink(1);
                }
                stack.setCount(Math.min(slot.getMaxStackSize(), Math.max(1, stack.getCount())));
                slot.set(stack);
            }
            return;
        }
        if (carried.isEmpty()) {
            if (!slot.getItem().isEmpty() && mouseButton == 1) {
                final ItemStack stack = slot.getItem();
                stack.setCount(input == ContainerInput.QUICK_MOVE
                    ? Math.min(slot.getMaxStackSize(), stack.getCount() * 2)
                    : stack.getCount() / 2);
                slot.set(stack);
            } else {
                slot.set(ItemStack.EMPTY);
            }
            return;
        }
        if (!slot.hasItem()) {
            final ItemStack stack = carried.copy();
            if (mouseButton == 1) {
                stack.setCount(1);
            }
            stack.setCount(Math.min(stack.getCount(), slot.getMaxStackSize()));
            slot.set(stack);
            return;
        }
        if (ItemIdentifier.get(carried).equals(ItemIdentifier.get(slot.getItem()))) {
            final ItemStack stack = slot.getItem();
            final int step = input == ContainerInput.QUICK_MOVE ? 10 : 1;
            if (mouseButton == 1) {
                stack.setCount(Math.min(slot.getMaxStackSize(), stack.getCount() + step));
                slot.set(stack);
            } else if (mouseButton == 0) {
                stack.shrink(step);
                slot.set(stack);
            }
            return;
        }
        final ItemStack stack = carried.copy();
        stack.setCount(Math.min(stack.getCount(), slot.getMaxStackSize()));
        slot.set(stack);
    }

    @Override
    public void addSlotListener(ContainerListener listener) {
        super.addSlotListener(listener);
        listeners.add(listener);
    }

    @Override
    public void removeSlotListener(ContainerListener listener) {
        super.removeSlotListener(listener);
        listeners.remove(listener);
    }

    /**
     * A drag either fills ghost slots or moves real items, never both: which one is decided by the
     * first slot the drag reaches.
     */
    @Override
    public boolean canDragTo(Slot slot) {
        if (slot instanceof UnmodifiableSlot || slot instanceof FluidSlot || slot instanceof ColorSlot
            || slot instanceof HandelableSlot) {
            return false;
        }
        final boolean firstOfThisDrag = lastDragLookup <= lastClicked;
        lastDragLookup = System.currentTimeMillis();
        if (firstOfThisDrag) {
            draggingGhostSlots = slot instanceof DummySlot;
            return true;
        }
        return slot instanceof DummySlot == draggingGhostSlots;
    }

    /** Sends the fuzzy flags of any slot whose flags changed, then the items as usual. */
    @Override
    public void broadcastChanges() {
        for (int i = 0; i < slots.size(); i++) {
            if (!(slots.get(i) instanceof IFuzzySlot fuzzySlot)) {
                continue;
            }
            final BitSet flags = fuzzySlot.getFuzzyFlags().copyValue();
            final BitSet known = slotsFuzzyFlags.get(i);
            if (known != null && known.equals(flags)) {
                continue;
            }
            final FuzzySlotFlagsMessage message = FuzzySlotFlagsMessage.of(fuzzySlot.getSlotId(), flags);
            listeners.stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .forEach(listener -> PacketDistributor.sendToPlayer(listener, message));
            slotsFuzzyFlags.set(i, flags);
        }
        super.broadcastChanges();
    }

    @Override
    protected Slot addSlot(Slot slot) {
        this.slotsFuzzyFlags.add(null);
        return super.addSlot(slot);
    }

    protected Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, Item item) {
        return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, item));
    }

    protected Slot addRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord, ISlotCheck slotCheck) {
        return addSlot(new RestrictedSlot(inventory, slotId, xCoord, yCoord, slotCheck));
    }

    /** A slot that shows a stack without holding one, for filters and recipe grids. */
    protected Slot addDummySlot(int slotId, Container inventory, int xCoord, int yCoord) {
        return addSlot(new DummySlot(inventory, slotId, xCoord, yCoord));
    }

    protected Slot addFuzzyDummySlot(int slotId, Container inventory, int xCoord, int yCoord, IBitSet fuzzyFlags) {
        return addSlot(new FuzzyDummySlot(inventory, slotId, xCoord, yCoord, fuzzyFlags));
    }

    /** The player's hotbar, shown but not reachable -- the screen only needs to display it. */
    protected void addRestrictedHotbarForPlayerInventory(Inventory inventory, int xOffset, int yOffset) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new UnmodifiableSlot(inventory, slot, xOffset + slot * 18, yOffset));
        }
    }

    /** The player's armour, likewise shown but not reachable. */
    protected void addRestrictedArmorForPlayerInventory(Inventory inventory, int xOffset, int yOffset) {
        for (int slot = 0; slot < 4; slot++) {
            addSlot(new UnmodifiableSlot(inventory, slot + 36, xOffset, yOffset - slot * 18));
        }
    }

    /** A slot that shows a fluid as its item form, and is never taken from. */
    protected Slot addFluidSlot(int slotId, Container inventory, int xCoord, int yCoord) {
        return addSlot(new FluidSlot(inventory, slotId, xCoord, yCoord));
    }

    /** A slot the player can take from but not put into, such as a crafting result. */
    protected Slot addUnmodifiableSlot(int slotId, Container inventory, int xCoord, int yCoord) {
        return addSlot(new UnmodifiableSlot(inventory, slotId, xCoord, yCoord));
    }

    protected Slot addFuzzyUnmodifiableSlot(int slotId, Container inventory, int xCoord, int yCoord,
        IBitSet fuzzyFlags) {
        return addSlot(new FuzzyUnmodifiableSlot(inventory, slotId, xCoord, yCoord, fuzzyFlags));
    }

    /** A slot whose click is answered by the pipe rather than by moving the stack. */
    protected Slot addCallableSlotHandler(int slotId, Container inventory, int xCoord, int yCoord,
        ISlotClick handler) {
        return addSlot(new HandelableSlot(inventory, slotId, xCoord, yCoord, handler));
    }

    /** A slot holding one of a pipe's sneaky upgrades, which live in their own inventory. */
    protected Slot addSneakyUpgradeSlot(int slotId, UpgradeManager manager, int upgradeSlotId, int xCoord,
        int yCoord, ISlotCheck slotCheck) {
        Slot slot = addSlot(new SneakyUpgradeSlot(manager, upgradeSlotId, slotId, xCoord, yCoord, slotCheck));
        transferTop.add(slot);
        return slot;
    }

    /** A restricted slot that also caps how much it holds. */
    protected Slot addStaticRestrictedSlot(int slotId, Container inventory, int xCoord, int yCoord,
        ISlotCheck slotCheck, int stackLimit) {
        return addSlot(new StaticRestrictedSlot(inventory, slotId, xCoord, yCoord, slotCheck, stackLimit));
    }

    /** A chassis module slot, which writes the module's settings back into the item when taken. */
    protected Slot addModuleSlot(int slotId, Container inventory, int xCoord, int yCoord,
        PipeLogisticsChassis pipe) {
        Slot slot = addSlot(new ModuleSlot(inventory, slotId, xCoord, yCoord, pipe));
        transferTop.add(slot);
        return slot;
    }

    /** A slot holding one of a module's or a pipe's upgrades. */
    protected Slot addUpgradeSlot(int slotId, ISlotUpgradeManager manager, int upgradeSlotId, int xCoord,
        int yCoord, ISlotCheck slotCheck) {
        Slot slot = addSlot(new UpgradeSlot(manager, upgradeSlotId, slotId, xCoord, yCoord, slotCheck));
        transferTop.add(slot);
        return slot;
    }

    /** An ordinary slot, which quick-move can also reach. */
    protected Slot addNormalSlot(int slotId, Container inventory, int xCoord, int yCoord) {
        Slot slot = addSlot(new Slot(inventory, slotId, xCoord, yCoord));
        transferTop.add(slot);
        return slot;
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
