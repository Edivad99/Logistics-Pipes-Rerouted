package logisticspipes.world.inventory;

import net.minecraft.world.entity.player.Inventory;

import lombok.Getter;

import logisticspipes.interfaces.IFreqCardHolder;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.world.item.LPItems;

/**
 * The single frequency card slot of an inventory system pipe.
 */
public class FreqCardMenu extends DummyMenu {

    @Getter
    private final IFreqCardHolder pipe;

    public FreqCardMenu(int containerId, Inventory inventory, IFreqCardHolder pipe) {
        super(LPMenuTypes.FREQ_CARD.get(), containerId, inventory.player, ((CoreUnroutedPipe) pipe).container);
        this.pipe = pipe;
        addRestrictedSlot(0, pipe.getFreqCardInventory(), 82, 15, LPItems.ITEM_CARD.get());
        addNormalSlotsForPlayerInventory(inventory, 10, 45);
    }
}
