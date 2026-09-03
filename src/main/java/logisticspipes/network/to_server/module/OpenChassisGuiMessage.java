package logisticspipes.network.to_server.module;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeLogisticsChassis;

/**
 * Goes back from a module's screen to the chassis holding it.
 *
 * <p>The way in is {@link OpenChassisModuleGuiMessage}; this is the way back out.
 */
public record OpenChassisGuiMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<OpenChassisGuiMessage> TYPE = new Type<>(LPConstants.rl("open_chassis_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChassisGuiMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenChassisGuiMessage::pos,
                    OpenChassisGuiMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenChassisGuiMessage message, IPayloadContext context) {
        final PipeLogisticsChassis chassis =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, PipeLogisticsChassis.class);
        if (chassis == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        IModuleMenuProvider.open(player, chassis.getLogisticsModule());
    }
}
