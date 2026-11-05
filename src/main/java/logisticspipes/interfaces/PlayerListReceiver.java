package logisticspipes.interfaces;

import java.util.List;

/**
 * A screen told the names of the players on the server.
 */
public interface PlayerListReceiver {

    void receivePlayerList(List<String> list);
}
