package logisticspipes.world.inventory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.interfaces.IWatchingHandler.WatchMode;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.IPipeUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.world.item.ItemUpgrade;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;

/**
 * The pipe controller: the pipe's own upgrades, its sneaky upgrades, its security card and the
 * disk slot, spread over tabs that the screen shows one at a time.
 *
 * <p>Every slot lives here rather than in the tabs, so that both sides agree on their order --
 * which is what the slot indices in the protocol mean -- and on where they are drawn.
 */
public class PipeControllerMenu extends DummyMenu {

    @Getter
    private final CoreRoutedPipe pipe;

    /** Nine pipe upgrades followed by nine sneaky upgrades, in tab order. */
    @Getter
    private final List<Slot> upgradeSlots;

    @Getter
    private final Slot securitySlot;

    @Getter
    private final Slot diskSlot;

    public PipeControllerMenu(int containerId, Inventory inventory, CoreRoutedPipe pipe) {
        super(LPMenuTypes.PIPE_CONTROLLER.get(), containerId, inventory.player, null);
        this.pipe = pipe;
        final UpgradeManager upgrades = pipe.getOriginalUpgradeManager();

        // The order the slots are added in is the order both sides index them by, so it has to
        // match the order the tabs claim them below.
        addNormalSlotsForPlayerInventory(inventory, 11, 136);

        final Slot[] slots = new Slot[18];
        for (int slot = 0; slot < 9; slot++) {
            slots[slot] = addUpgradeSlot(slot, upgrades, slot, 10 + slot * 18, 42,
                stack -> allowsUpgrade(stack, pipe));
        }
        for (int slot = 0; slot < 9; slot++) {
            slots[slot + 9] = addSneakyUpgradeSlot(slot, upgrades, slot + 9, 10 + slot * 18, 88,
                stack -> allowsSneakyUpgrade(stack, pipe));
        }
        upgradeSlots = List.of(slots);

        securitySlot = addStaticRestrictedSlot(0, upgrades.secInv, 10, 42,
            PipeControllerMenu::isAuthorizedCard, 1);
        // Kept but hidden; it may be used again once the logic controller tab comes back.
        diskSlot = addRestrictedSlot(0, pipe.container.logicController.diskInv, 14, 36, LPItems.DISK.get());

        if (inventory.player instanceof ServerPlayer) {
            upgrades.getGuiController().screenOpenedByPlayer(inventory.player);
            // The statistics tab is fed by the pipe while someone is watching it.
            pipe.playerStartWatching(inventory.player, WatchMode.GUI);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            final IScreenOpenController controller = pipe.getOriginalUpgradeManager().getGuiController();
            controller.screenClosedByPlayer(player);
            pipe.playerStopWatching(player, WatchMode.GUI);
        }
    }

    private static boolean allowsUpgrade(ItemStack stack, CoreRoutedPipe pipe) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemUpgrade upgrade
            && upgrade.getUpgradeForItem(stack, null).isAllowedForPipe(pipe);
    }

    private static boolean allowsSneakyUpgrade(ItemStack stack, CoreRoutedPipe pipe) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemUpgrade item)) {
            return false;
        }
        final IPipeUpgrade upgrade = item.getUpgradeForItem(stack, null);
        return upgrade instanceof SneakyUpgradeConfig && upgrade.isAllowedForPipe(pipe);
    }

    private static boolean isAuthorizedCard(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(LPItems.SECURITY_CARD)) {
            return false;
        }
        final UUID uuid = Objects.requireNonNull(stack.get(LPDataComponents.UUID));
        return SimpleServiceLocator.securityStationManager.isAuthorized(uuid);
    }
}
