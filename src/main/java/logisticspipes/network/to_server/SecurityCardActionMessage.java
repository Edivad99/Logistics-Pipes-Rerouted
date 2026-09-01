package logisticspipes.network.to_server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity.CardAction;
import logisticspipes.network.TargetLookup;

/**
 * The player pressed one of the security station's card buttons.
 *
 * <p>The action is named rather than numbered: the four buttons used to travel as 0 to 3, which the
 * station turned back into behaviour with a bare {@code switch}.
 */
public record SecurityCardActionMessage(BlockPos pos, CardAction action) implements CustomPacketPayload {

    public static final Type<SecurityCardActionMessage> TYPE =
            new Type<>(LPConstants.rl("security_card_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityCardActionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SecurityCardActionMessage::pos,
                    NeoForgeStreamCodecs.enumCodec(CardAction.class), SecurityCardActionMessage::action,
                    SecurityCardActionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityCardActionMessage message, IPayloadContext context) {
        final LogisticsSecurityTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsSecurityTileEntity.class);
        if (be != null) {
            be.handleCardAction(message.action, context.player());
        }
    }
}
