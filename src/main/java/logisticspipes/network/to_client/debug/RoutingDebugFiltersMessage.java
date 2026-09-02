package logisticspipes.network.to_client.debug;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.debug.ClientViewController;

/**
 * Where the filters sit on each way of reaching a pipe.
 *
 * <p>A map from connection type to chains of filter positions. The old format wrote it as two
 * nested loops terminated by {@code -1} shorts, which meant a filter chain of 32767 entries or a
 * type ordinal out of range was read as a terminator or an array index.
 */
public record RoutingDebugFiltersMessage(BlockPos pos, Map<PipeRoutingConnectionType, List<List<BlockPos>>> filters)
        implements CustomPacketPayload {

    public static final Type<RoutingDebugFiltersMessage> TYPE =
            new Type<>(LPConstants.rl("routing_debug_filters"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<PipeRoutingConnectionType, List<List<BlockPos>>>>
            FILTERS_CODEC = ByteBufCodecs.map(
                    size -> new EnumMap<>(PipeRoutingConnectionType.class),
                    NeoForgeStreamCodecs.enumCodec(PipeRoutingConnectionType.class),
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugFiltersMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RoutingDebugFiltersMessage::pos,
                    FILTERS_CODEC, RoutingDebugFiltersMessage::filters,
                    RoutingDebugFiltersMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugFiltersMessage message, IPayloadContext context) {
        ClientViewController.instance().setFilters(message.pos, message.filters);
    }
}
