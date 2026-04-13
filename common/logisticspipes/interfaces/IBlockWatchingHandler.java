package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IBlockWatchingHandler {

	void playerStartWatching(Player player);

	void playerStopWatching(Player player);
}
