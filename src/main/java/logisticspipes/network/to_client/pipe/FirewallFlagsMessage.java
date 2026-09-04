package logisticspipes.network.to_client.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The firewall's current switches, sent to the player as their GUI opens.
 */
public record FirewallFlagsMessage(BlockPos pos, PipeItemsFirewall.FirewallFlags flags) implements CustomPacketPayload {

    public static final Type<FirewallFlagsMessage> TYPE =
            new Type<>(LPConstants.rl("firewall_flags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirewallFlagsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FirewallFlagsMessage::pos,
                    PipeItemsFirewall.FirewallFlags.STREAM_CODEC, FirewallFlagsMessage::flags,
                    FirewallFlagsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FirewallFlagsMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container != null && container.pipe instanceof PipeItemsFirewall firewall) {
            firewall.setFlags(message.flags);
        }
    }
}
