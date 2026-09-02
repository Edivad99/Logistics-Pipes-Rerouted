package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import lombok.Getter;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;
import logisticspipes.utils.gui.UnmodifiableSlot;

/**
 * A module's settings screen.
 *
 * <p>One menu covers all three places a module can be -- in a pipe, in a chassis slot, or held in
 * hand -- because {@link ModuleTarget} already knows how to find it in each. The GUI system this
 * replaces needed two providers per module, one for the pipe and one for the hand, differing only
 * in that lookup.
 */
public abstract class ModuleMenu extends DummyMenu {

    @Getter
    private final ModuleTarget target;

    @Getter
    private final LogisticsModule module;

    protected ModuleMenu(MenuType<?> menuType, int containerId, Inventory inventory, ModuleTarget target,
        LogisticsModule module) {
        super(menuType, containerId, inventory.player, null);
        this.target = target;
        this.module = module;
    }

    private boolean heldInHand() {
        return target.slot().orElse(null) == ModulePositionType.IN_HAND;
    }

    /**
     * The module being configured cannot be picked up while its own screen is open.
     */
    @Override
    protected Slot addSlot(Slot slot) {
        if (heldInHand() && slot.container == getPlayer().getInventory()
            && slot.getSlotIndex() == target.positionInt()) {
            return super.addSlot(new UnmodifiableSlot(slot));
        }
        return super.addSlot(slot);
    }

    /**
     * A module held in hand has nowhere to live but its item, so what was configured is written
     * back when the screen closes.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (heldInHand()) {
            ItemModuleInformationManager.saveInformation(
                player.getInventory().getItem(target.positionInt()), module, player.registryAccess());
            player.getInventory().setChanged();
        }
    }
}
