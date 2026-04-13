package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IModuleWatchReciver {

	void startWatching(Player player);

	void stopWatching(Player player);
}
