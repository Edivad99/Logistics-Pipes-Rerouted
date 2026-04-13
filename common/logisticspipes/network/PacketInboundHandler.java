package logisticspipes.network;

// In NeoForge 1.20.1, packet dispatching is handled via IPayloadHandler lambdas
// registered in LogisticsPipes.registerPayloads() — see PacketHandler.handleServer()
// and PacketHandler.handleClient(). This class is no longer needed and is kept only
// as a placeholder to avoid breaking any import references.

/**
 * @deprecated Replaced by {@link PacketHandler#handleServer} and {@link PacketHandler#handleClient}
 *             registered as NeoForge IPayloadHandler lambdas.
 */
@Deprecated
public final class PacketInboundHandler {
    private PacketInboundHandler() {}
}
