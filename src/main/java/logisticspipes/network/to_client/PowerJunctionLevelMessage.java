package logisticspipes.network.to_client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

/**
 * How much power a junction is holding, for its GUI and its HUD.
 */
public record PowerJunctionLevelMessage(BlockPos pos, int stored) implements CustomPacketPayload {

    public static final Type<PowerJunctionLevelMessage> TYPE =
            new Type<>(LPConstants.rl("power_junction_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerJunctionLevelMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PowerJunctionLevelMessage::pos,
                    ByteBufCodecs.VAR_INT, PowerJunctionLevelMessage::stored,
                    PowerJunctionLevelMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PowerJunctionLevelMessage message, IPayloadContext context) {
        final LogisticsPowerJunctionBlockEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsPowerJunctionBlockEntity.class);
        if (be != null) {
            be.handlePowerPacket(message.stored);
        }
    }
}
