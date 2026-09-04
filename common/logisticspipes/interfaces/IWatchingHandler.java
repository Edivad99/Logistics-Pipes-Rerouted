package logisticspipes.interfaces;

import net.minecraft.world.entity.player.Player;

public interface IWatchingHandler {

    void playerStartWatching(Player player, WatchMode mode);

    void playerStopWatching(Player player, WatchMode mode);

    /**
     * What a player is watching a pipe with. The two views want different data, so a pipe answers
     * only for the one it serves and passes the other up to its superclass.
     */
    enum WatchMode {
        /**
         * The pipe's GUI is open: the watcher wants the routing statistics.
         */
        GUI,
        /**
         * The head-up display is showing the pipe: the watcher wants the pipe's own content.
         */
        HUD,
    }
}
