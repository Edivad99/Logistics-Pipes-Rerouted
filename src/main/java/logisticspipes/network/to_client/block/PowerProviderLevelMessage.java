package logisticspipes.network.to_client.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.network.TargetLookup;

/**
 * How much power a provider is holding, for its GUI and its HUD.
 *
 * <p>A double rather than the junction's int because the providers keep fractional amounts while
 * converting between their own unit and the network's.
 */
public record PowerProviderLevelMessage(BlockPos pos, double stored) implements CustomPacketPayload {

    public static final Type<PowerProviderLevelMessage> TYPE =
            new Type<>(LPConstants.rl("power_provider_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerProviderLevelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PowerProviderLevelMessage::pos,
                    ByteBufCodecs.DOUBLE, PowerProviderLevelMessage::stored,
                    PowerProviderLevelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PowerProviderLevelMessage message, IPayloadContext context) {
        final LogisticsPowerProviderTileEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsPowerProviderTileEntity.class);
        if (be != null) {
            be.handlePowerPacket(message.stored);
        }
    }
}
