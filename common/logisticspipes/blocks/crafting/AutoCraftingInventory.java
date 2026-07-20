package logisticspipes.blocks.crafting;

import logisticspipes.utils.PlayerIdentifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

public class AutoCraftingInventory extends TransientCraftingContainer {

	public final PlayerIdentifier placedByPlayer;

	public AutoCraftingInventory(PlayerIdentifier playerID) {
		super(new AbstractContainerMenu(null, 0) {
			@Override
			public boolean stillValid(Player entityplayer) { return false; }
			@Override
			public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
		}, 3, 3);
		placedByPlayer = playerID;
	}
}
