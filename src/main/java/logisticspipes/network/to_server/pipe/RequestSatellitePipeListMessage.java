package logisticspipes.network.to_server.pipe;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.network.TargetLookup;
import logisticspipes.LPConstants;
import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.ProvideSatellitePipeListPacket;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.utils.tuples.Pair;

/**
 * The satellite picker popup asking for the satellites this pipe can actually reach.
 *
 * @param pos   the pipe the popup was opened on
 * @param fluid fluid satellites when true, item satellites when false
 */
public record RequestSatellitePipeListMessage(BlockPos pos, boolean fluid) implements CustomPacketPayload {

    public static final Type<RequestSatellitePipeListMessage> TYPE =
            new Type<>(LPConstants.rl("request_satellite_pipe_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSatellitePipeListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RequestSatellitePipeListMessage::pos,
                    ByteBufCodecs.BOOL, RequestSatellitePipeListMessage::fluid,
                    RequestSatellitePipeListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSatellitePipeListMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null
                || !(container.pipe instanceof CoreRoutedPipe pipe)
                || pipe.getRouter() == null
                || pipe.getRouter().getRouteTable() == null) {
            return;
        }
        final List<Pair<String, UUID>> satellites = message.fluid
                ? reachableFrom(pipe, PipeFluidSatellite.AllSatellites)
                : reachableFrom(pipe, PipeItemsSatelliteLogistics.AllSatellites);
        PacketHandler.sendToPlayer(
            PacketHandler.getPacket(ProvideSatellitePipeListPacket.class).setList(satellites), context.player());
    }

    /**
     * The satellites {@code from} has a route to, nearest first.
     *
     * <p>One method for both kinds: the two branches this replaced were the same twenty lines
     * twice over, differing only in which set they read.
     */
    private static <T extends CoreRoutedPipe & SatellitePipe> List<Pair<String, UUID>> reachableFrom(
            CoreRoutedPipe from,
            Collection<T> satellites
    ) {
        final List<List<ExitRoute>> routeTable = from.getRouter().getRouteTable();
        return satellites.stream()
                .filter(Objects::nonNull)
                .filter(satellite -> satellite.getRouter() != null)
                .filter(satellite -> routesTo(routeTable, satellite.getRouterId()))
                .sorted(Comparator.comparingDouble(satellite -> nearestHop(routeTable, satellite.getRouterId())))
                .map(satellite -> new Pair<>(satellite.getSatellitePipeName(), satellite.getRouter().getId()))
                .collect(Collectors.toList());
    }

    private static boolean routesTo(List<List<ExitRoute>> routeTable, int routerId) {
        return routeTable.size() > routerId
                && routeTable.get(routerId) != null
                && !routeTable.get(routerId).isEmpty();
    }

    private static double nearestHop(List<List<ExitRoute>> routeTable, int routerId) {
        return routeTable.get(routerId).stream()
                .mapToDouble(route -> route.distanceToDestination)
                .min()
                .orElse(Double.MAX_VALUE);
    }
}
