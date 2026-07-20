package logisticspipes.utils.gui;

import logisticspipes.interfaces.ISlotClick;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HandelableSlot extends Slot {

	private final ISlotClick _handler;

	public HandelableSlot(Container inventory, int slotId, int xCoord, int yCoord, ISlotClick handler) {
		super(inventory, slotId, xCoord, yCoord);
		_handler = handler;
	}

	@Override
	public boolean mayPlace(ItemStack par1ItemStack) {
		return par1ItemStack.isEmpty();
	}

	public ItemStack getProvidedStack() {
		return _handler.getResultForClick();
	}

	@Override
	public boolean mayPickup(Player p_82869_1_) {
		return false;
	}

}
