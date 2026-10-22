package logisticspipes.network.to_client.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsPowerProviderBlockEntity;

/**
 * How much power a provider is holding, for its GUI and its HUD.
 *
 * <p>A double rather than the junction's int because the providers keep fractional amounts while
 * converting between their own unit and the network's.
 */
public record PowerProviderLevelMessage(BlockPos pos, int stored) implements CustomPacketPayload {

    public static final Type<PowerProviderLevelMessage> TYPE =
        new Type<>(LPConstants.rl("power_provider_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerProviderLevelMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, PowerProviderLevelMessage::pos,
            ByteBufCodecs.VAR_INT, PowerProviderLevelMessage::stored,
            PowerProviderLevelMessage::new);

    public static void handle(PowerProviderLevelMessage message, IPayloadContext context) {
        final LogisticsPowerProviderBlockEntity be = TargetLookup.blockEntityAt(
            context.player(), message.pos, LogisticsPowerProviderBlockEntity.class);
        if (be != null) {
            be.handlePowerPacket(message.stored);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
