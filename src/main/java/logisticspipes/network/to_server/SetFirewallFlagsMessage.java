package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.network.TargetLookup;
import logisticspipes.LPConstants;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * The player toggled one of the firewall's five switches.
 *
 * <p>All five travel every time, as they always have: the GUI edits one at a time but the pipe
 * has no notion of a partial update.
 */
public record SetFirewallFlagsMessage(BlockPos pos, PipeItemsFirewall.FirewallFlags flags) implements CustomPacketPayload {

    public static final Type<SetFirewallFlagsMessage> TYPE =
            new Type<>(LPConstants.rl("set_firewall_flags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFirewallFlagsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetFirewallFlagsMessage::pos,
                    PipeItemsFirewall.FirewallFlags.STREAM_CODEC, SetFirewallFlagsMessage::flags,
                    SetFirewallFlagsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFirewallFlagsMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container != null && container.pipe instanceof PipeItemsFirewall firewall) {
            firewall.setFlags(message.flags);
        }
    }
}
