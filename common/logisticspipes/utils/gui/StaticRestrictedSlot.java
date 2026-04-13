package logisticspipes.utils.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;

import logisticspipes.interfaces.ISlotCheck;

public class StaticRestrictedSlot extends RestrictedSlot {

	int limit;

	public StaticRestrictedSlot(Container iinventory, int i, int j, int k, Item item, int stackLimit) {
		super(iinventory, i, j, k, item);
		limit = stackLimit;
	}

	public StaticRestrictedSlot(Container iinventory, int i, int j, int k, ISlotCheck slotCheck, int stackLimit) {
		super(iinventory, i, j, k, slotCheck);
		limit = stackLimit;
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	@Override
	public boolean mayPickup(Player par1Player) {
		return false;
	}

	/**
	 * Returns the maximum stack size for a given slot (usually the same as
	 * getMaxStackSize(), but 1 in the case of armor slots)
	 */
	@Override
	public int getMaxStackSize() {
		return limit;
	}
}
