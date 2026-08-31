package logisticspipes.network.to_client;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;

/**
 * The computer ids a security station excludes.
 *
 * <p>They used to travel as a {@code CompoundTag} holding a single {@code ListTag} of ints, built
 * and taken apart by hand at either end.
 */
public record SecurityStationCCIdsMessage(BlockPos pos, List<Integer> excludedIds)
        implements CustomPacketPayload {

    public static final Type<SecurityStationCCIdsMessage> TYPE =
            new Type<>(LPConstants.rl("security_station_cc_ids"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SecurityStationCCIdsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SecurityStationCCIdsMessage::pos,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SecurityStationCCIdsMessage::excludedIds,
                    SecurityStationCCIdsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SecurityStationCCIdsMessage message, IPayloadContext context) {
        final BlockEntity be = context.player().level().getBlockEntity(message.pos);
        if (be instanceof LogisticsSecurityTileEntity station) {
            station.setExcludedCC(message.excludedIds);
        }
    }
}
