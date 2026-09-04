package logisticspipes.network.to_client.debug;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.DebugTarget;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Turns the pipe log on or off for the client's own copy of the pipe the player is pointing at.
 *
 * <p>Nothing goes back: the client half of a pipe keeps its own state, so the toggle happens where
 * the answer would have been sent from.
 */
public record ToggleClientPipeDebugMessage() implements CustomPacketPayload {

    public static final Type<ToggleClientPipeDebugMessage> TYPE =
            new Type<>(LPConstants.rl("toggle_client_pipe_debug"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleClientPipeDebugMessage> STREAM_CODEC =
            StreamCodec.unit(new ToggleClientPipeDebugMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleClientPipeDebugMessage message, IPayloadContext context) {
        if (!(DebugTarget.lookedAt() instanceof DebugTarget.Block block)) {
            return;
        }
        final LogisticsTileGenericPipe be =
                TargetLookup.blockEntityAt(context.player(), block.pos(), LogisticsTileGenericPipe.class);
        if (be == null || be.pipe == null) {
            return;
        }
        be.pipe.debug.debugThisPipe = !be.pipe.debug.debugThisPipe;
        context.player().sendSystemMessage(Component.literal(
                be.pipe.debug.debugThisPipe ? "Debug enabled on client" : "Debug disabled on client"));
    }
}
