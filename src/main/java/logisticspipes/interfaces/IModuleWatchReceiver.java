package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

/**
 * A module that sends updates to the players watching it.
 */
public interface IModuleWatchReceiver {

    void startWatching(Player player);

    void stopWatching(Player player);
}
