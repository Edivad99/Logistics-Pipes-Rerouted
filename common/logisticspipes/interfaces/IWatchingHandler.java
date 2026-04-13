package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IWatchingHandler {

	void playerStartWatching(Player player, int mode);

	void playerStopWatching(Player player, int mode);
}
