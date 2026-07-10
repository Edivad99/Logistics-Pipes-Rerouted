/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.gui;

import lombok.Setter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class DummySlot extends Slot {

	@Setter
	private boolean redirectCall = false;

	public DummySlot(Container iinventory, int i, int j, int k) {
		super(iinventory, i, j, k);
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
