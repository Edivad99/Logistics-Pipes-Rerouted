package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * The player toggled one of the firewall's five switches.
 *
 * <p>All five travel every time, as they always have: the GUI edits one at a time but the pipe
 * has no notion of a partial update.
 */
public record SetFirewallFlagsMessage(BlockPos pos, PipeItemsFirewall.FirewallFlags flags)
    implements CustomPacketPayload {

    public static final Type<SetFirewallFlagsMessage> TYPE =
        new Type<>(LPConstants.rl("set_firewall_flags"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFirewallFlagsMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetFirewallFlagsMessage::pos,
            PipeItemsFirewall.FirewallFlags.STREAM_CODEC, SetFirewallFlagsMessage::flags,
            SetFirewallFlagsMessage::new);

    public static void handle(SetFirewallFlagsMessage message, IPayloadContext context) {
        final LogisticsGenericPipeBlockEntity container =
            TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsGenericPipeBlockEntity.class);
        if (container != null && container.pipe instanceof PipeItemsFirewall firewall) {
            firewall.setFlags(message.flags);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
