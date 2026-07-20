package logisticspipes.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * public interface to be implemented by an item which can open the config GUI for a logistics pipe.
 * Some mod compatibility is already implemented inside LP.
 */
public interface ILPPipeConfigTool {

	boolean canWrench(Player player, ItemStack wrench, ILPPipeTile pipe);

	void wrenchUsed(Player player, ItemStack wrench, ILPPipeTile pipe);
}
