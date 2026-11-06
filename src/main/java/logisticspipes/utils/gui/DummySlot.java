package logisticspipes.utils.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import lombok.Setter;

public class DummySlot extends Slot {

    @Setter
    private boolean redirectCall = false;

    public DummySlot(Container container, int i, int j, int k) {
        super(container, i, j, k);
    }

    @Override
    public boolean mayPickup(Player par1Player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        if (redirectCall) {
            return super.getMaxStackSize();
        }
        return 0;
    }
}
