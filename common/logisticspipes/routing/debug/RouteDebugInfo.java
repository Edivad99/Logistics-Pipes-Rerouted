package logisticspipes.routing.debug;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;

/**
 * One route, as the routing debugger draws it.
 *
 * <p>An {@link ExitRoute} is a live server object: it points at two routers and a list of filters,
 * none of which exist on a client. It used to be sent whole anyway, and rebuilt by looking the
 * routers back up from their coordinates -- which meant loading chunks from a debug message, and
 * throwing when the pipe was not there. This carries what the debugger actually draws instead.
 *
 * @param destinationName    what the router prints as, for the debug window's list
 * @param networkDescription the route's own summary line
 * @param index              its place in the candidate list, or -1
 */
public record RouteDebugInfo(
        BlockPos destination,
        String destinationName,
        String networkDescription,
        int index,
        boolean newlyAddedCandidate,
        Set<PipeRoutingConnectionType> flags,
        List<BlockPos> filterPositions
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RouteDebugInfo> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RouteDebugInfo::destination,
                    ByteBufCodecs.STRING_UTF8, RouteDebugInfo::destinationName,
                    ByteBufCodecs.STRING_UTF8, RouteDebugInfo::networkDescription,
                    ByteBufCodecs.VAR_INT, RouteDebugInfo::index,
                    ByteBufCodecs.BOOL, RouteDebugInfo::newlyAddedCandidate,
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, PipeRoutingConnectionType>enumCodec(
                            PipeRoutingConnectionType.class).apply(ByteBufCodecs.collection(java.util.HashSet::new)),
                    RouteDebugInfo::flags,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), RouteDebugInfo::filterPositions,
                    RouteDebugInfo::new);

    public static RouteDebugInfo of(ExitRoute route) {
        final List<BlockPos> filters = route.debug.filterPosition == null
                ? List.of()
                : route.debug.filterPosition.stream().map(pos -> pos.getBlockPos()).toList();
        return new RouteDebugInfo(
                route.destination.getLPPosition().getBlockPos(),
                route.destination.toString(),
                route.debug.toStringNetwork == null ? "" : route.debug.toStringNetwork,
                route.debug.index,
                route.debug.isNewlyAddedCanidate,
                route.getFlags(),
                filters);
    }
}
