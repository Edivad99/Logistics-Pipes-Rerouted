package logisticspipes.network.to_server.security;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.TargetLookup;

/**
 * The player name typed into a security station's search bar: open their settings.
 */
public record OpenSecurityPlayerMessage(BlockPos pos, String playerName) implements CustomPacketPayload {

    public static final Type<OpenSecurityPlayerMessage> TYPE =
            new Type<>(LPConstants.rl("open_security_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSecurityPlayerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenSecurityPlayerMessage::pos,
                    ByteBufCodecs.STRING_UTF8, OpenSecurityPlayerMessage::playerName,
                    OpenSecurityPlayerMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenSecurityPlayerMessage message, IPayloadContext context) {
        if (message.playerName.isEmpty()) {
            return;
        }
        final LogisticsSecurityTileEntity station =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (station != null) {
            station.handleOpenSecurityPlayer(context.player(), message.playerName);
        }
    }
}
