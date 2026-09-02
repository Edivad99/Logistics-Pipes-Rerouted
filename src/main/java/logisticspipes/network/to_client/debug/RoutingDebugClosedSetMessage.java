package logisticspipes.network.to_client.debug;

import java.util.HashSet;
import java.util.Set;

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

/** Which ways of reaching a pipe the debugger has already settled. */
public record RoutingDebugClosedSetMessage(BlockPos pos, Set<PipeRoutingConnectionType> closed)
        implements CustomPacketPayload {

    public static final Type<RoutingDebugClosedSetMessage> TYPE =
            new Type<>(LPConstants.rl("routing_debug_closed_set"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoutingDebugClosedSetMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RoutingDebugClosedSetMessage::pos,
                    NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, PipeRoutingConnectionType>enumCodec(
                            PipeRoutingConnectionType.class).apply(ByteBufCodecs.collection(HashSet::new)),
                    RoutingDebugClosedSetMessage::closed,
                    RoutingDebugClosedSetMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoutingDebugClosedSetMessage message, IPayloadContext context) {
        ClientViewController.instance().setClosedSet(message.pos, message.closed);
    }
}
