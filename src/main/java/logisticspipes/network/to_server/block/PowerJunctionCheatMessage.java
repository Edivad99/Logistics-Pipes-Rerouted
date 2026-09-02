package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.TargetLookup;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

/**
 * Fills a power junction, from the button the debug builds add to its screen.
 */
public record PowerJunctionCheatMessage(BlockPos pos) implements CustomPacketPayload {

    private static final int ENERGY = 100_000;

    public static final Type<PowerJunctionCheatMessage> TYPE = new Type<>(LPConstants.rl("power_junction_cheat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerJunctionCheatMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PowerJunctionCheatMessage::pos,
                    PowerJunctionCheatMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PowerJunctionCheatMessage message, IPayloadContext context) {
        if (!LogisticsPipes.isDEBUG()) {
            return;
        }
        final LogisticsPowerJunctionBlockEntity be = TargetLookup.blockEntityAt(
                context.player(), message.pos, LogisticsPowerJunctionBlockEntity.class);
        if (be != null) {
            be.addEnergy(ENERGY);
        }
    }
}
