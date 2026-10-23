package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * The routers this client has seen, so the HUD can find the pipes around the player.
 *
 * <p>Separate from {@link RouterManager} because the two never shared anything: the client list
 * holds {@link ClientRouter}s, is filled by the pipes themselves and emptied when a level loads,
 * while the server list is a different type under a different lock with its own uuid index. Keeping
 * both in one object meant that object had to work out which of its two callers it was serving, and
 * with a single manager shared by both sides of a singleplayer game the only thing left to ask was
 * the name of the calling thread.
 */
public final class ClientRouterManager {

    private static final ClientRouterManager INSTANCE = new ClientRouterManager();

    private final List<IRouter> routers = new ArrayList<>();

    private ClientRouterManager() {
    }

    public static ClientRouterManager get() {
        return INSTANCE;
    }

    /** The router for the pipe at these coordinates, created on first sight. */
    public IRouter getOrCreateRouter(UUID id, Level level, BlockPos pos) {
        final Identifier dimension = level.dimension().identifier();
        synchronized (routers) {
            for (IRouter existing : routers) {
                if (existing.isAt(dimension, pos)) {
                    return existing;
                }
            }
            final IRouter router = new ClientRouter(id, dimension, pos);
            routers.add(router);
            return router;
        }
    }

    public List<IRouter> getRouters() {
        return Collections.unmodifiableList(routers);
    }

    public void clear() {
        synchronized (routers) {
            routers.clear();
        }
    }
}
