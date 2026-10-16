package logisticspipes.proxy.specialconnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import logisticspipes.interfaces.routing.ISpecialPipedConnection;
import logisticspipes.interfaces.routing.ISpecialBlockEntityConnection;

/**
 * Fired on the mod event bus so addons can join two points of the network that are not neighbours --
 * a pair of linked blocks that items travel between without a pipe in between.
 *
 * <p>It is fired once during startup, and both sets are closed when it returns.
 *
 * <p>Not part of {@code src/api} yet: both interfaces still speak in terms of internal routing types.
 */
public class RegisterSpecialConnectionsEvent extends Event implements IModBusEvent {

    private final List<ISpecialBlockEntityConnection> blockEntityConnections = new ArrayList<>();
    private final List<ISpecialPipedConnection> pipedConnections = new ArrayList<>();

    /** Adds a handler joining plain blocks. Handlers are asked in order, and the first match answers. */
    public void register(ISpecialBlockEntityConnection connectionHandler) {
        blockEntityConnections.add(Objects.requireNonNull(connectionHandler, "connectionHandler"));
    }

    /** Adds a handler joining pipes. Handlers are asked in order, and the first match answers. */
    public void register(ISpecialPipedConnection connectionHandler) {
        pipedConnections.add(Objects.requireNonNull(connectionHandler, "connectionHandler"));
    }

    List<ISpecialBlockEntityConnection> registeredBlockEntityConnections() {
        return List.copyOf(blockEntityConnections);
    }

    List<ISpecialPipedConnection> registeredPipedConnections() {
        return List.copyOf(pipedConnections);
    }
}
