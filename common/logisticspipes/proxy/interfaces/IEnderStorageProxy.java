package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IEnderStorageProxy {

	boolean isEnderChestBlock(Block block);

	void openEnderChest(Level world, int x, int y, int z, Player player);
}
