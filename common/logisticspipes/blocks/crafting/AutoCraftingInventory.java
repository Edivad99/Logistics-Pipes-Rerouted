package logisticspipes.blocks.crafting;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;

import logisticspipes.utils.PlayerIdentifier;

public class AutoCraftingInventory extends TransientCraftingContainer {

	public final PlayerIdentifier placedByPlayer;

	public AutoCraftingInventory(PlayerIdentifier playerID) {
		super(new AbstractContainerMenu(null, 0) {
			@Override
			public boolean stillValid(@Nonnull Player entityplayer) { return false; }
			@Override
			public net.minecraft.world.item.ItemStack quickMoveStack(@Nonnull Player player, int i) { return net.minecraft.world.item.ItemStack.EMPTY; }
		}, 3, 3);
		placedByPlayer = playerID;
	}
}
