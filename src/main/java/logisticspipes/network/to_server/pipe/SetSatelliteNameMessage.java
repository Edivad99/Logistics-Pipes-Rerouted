package logisticspipes.network.to_server.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.network.TargetLookup;
import logisticspipes.network.to_client.pipe.SatelliteNameResultMessage;
import logisticspipes.pipes.SatelliteNamingResult;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * A new name typed into a satellite pipe's GUI.
 *
 * <p>The answer goes back either way -- accepted, blank, or already taken -- because the GUI
 * shows it to the player.
 */
public record SetSatelliteNameMessage(BlockPos pos, String name) implements CustomPacketPayload {

    public static final Type<SetSatelliteNameMessage> TYPE =
            new Type<>(LPConstants.rl("set_satellite_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSatelliteNameMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetSatelliteNameMessage::pos,
                    ByteBufCodecs.STRING_UTF8, SetSatelliteNameMessage::name,
                    SetSatelliteNameMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetSatelliteNameMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null) {
            return;
        }
        final SatelliteNamingResult result = rename(container, message.name);
        if (result != null) {
            if (context.player() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new SatelliteNameResultMessage(result, message.name));
            }
        }
    }

    /** The outcome to report back, or null when the target is not a satellite at all. */
    private static @Nullable SatelliteNamingResult rename(LogisticsTileGenericPipe container, String name) {
        if (name.trim().isEmpty()) {
            return SatelliteNamingResult.BLANK_NAME;
        }
        if (!(container.pipe instanceof SatellitePipe satellite)) {
            return null;
        }
        if (satellite.getSatellitesOfType().stream().anyMatch(it -> it.getSatellitePipeName().equals(name))) {
            return SatelliteNamingResult.DUPLICATE_NAME;
        }
        satellite.setSatellitePipeName(name);
        satellite.updateWatchers();
        satellite.ensureAllSatelliteStatus();
        return SatelliteNamingResult.SUCCESS;
    }
}
