package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

/**
 * Fills a power junction, from the button the debug builds add to its screen.
 *
 * @param toMax whether to fill it up completely rather than by a fixed step, as shift-clicking does
 */
public record PowerJunctionCheatMessage(BlockPos pos, boolean toMax) implements CustomPacketPayload {

    public static final Type<PowerJunctionCheatMessage> TYPE = new Type<>(LPConstants.rl("power_junction_cheat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerJunctionCheatMessage> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, PowerJunctionCheatMessage::pos,
            ByteBufCodecs.BOOL, PowerJunctionCheatMessage::toMax,
            PowerJunctionCheatMessage::new);
    private static final int ENERGY = 100_000;

    public static void handle(PowerJunctionCheatMessage message, IPayloadContext context) {
        if (!LogisticsPipes.isDEBUG()) {
            return;
        }
        final LogisticsPowerJunctionBlockEntity be = TargetLookup.blockEntityAt(
            context.player(), message.pos, LogisticsPowerJunctionBlockEntity.class);
        if (be != null) {
            be.addEnergy(message.toMax ? LogisticsPowerJunctionBlockEntity.MAX_STORAGE : ENERGY);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
