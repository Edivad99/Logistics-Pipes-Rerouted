package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

/**
 * A block that sends updates to the players whose HUD is showing it.
 */
public interface IBlockWatchingHandler {

    void playerStartWatching(Player player);

    void playerStopWatching(Player player);
}
