package logisticspipes.security;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The client's copy of the security stations the server has authorized, by id.
 *
 * <p>Kept apart from the server's list in {@link logisticspipes.routing.RouterManager}: in a
 * singleplayer game both sides run in one process, and a client copy stored in the same place would
 * replace the server's own list.
 */
public final class ClientAuthorizations {

    private static volatile List<String> authorized = List.of();

    private ClientAuthorizations() {
    }

    public static void set(List<String> stationIds) {
        authorized = List.copyOf(stationIds);
    }

    public static boolean isAuthorized(@Nullable UUID stationId) {
        return stationId != null && authorized.contains(stationId.toString());
    }
}
