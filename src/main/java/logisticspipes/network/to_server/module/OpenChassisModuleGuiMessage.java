package logisticspipes.network.to_server.module;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IModuleMenuProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.PipeLogisticsChassis;

import network.rs485.logisticspipes.module.LegacyModuleGui;

/**
 * Opens the settings screen of one module sitting in a chassis.
 *
 * <p>The chassis screen shows a button per slot; which module is in that slot is the server's to
 * know.
 */
public record OpenChassisModuleGuiMessage(BlockPos pos, int slot) implements CustomPacketPayload {

    public static final Type<OpenChassisModuleGuiMessage> TYPE =
            new Type<>(LPConstants.rl("open_chassis_module_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChassisModuleGuiMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenChassisModuleGuiMessage::pos,
                    ByteBufCodecs.VAR_INT, OpenChassisModuleGuiMessage::slot,
                    OpenChassisModuleGuiMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenChassisModuleGuiMessage message, IPayloadContext context) {
        final PipeLogisticsChassis chassis =
                TargetLookup.blockEntityOrPipeAt(context.player(), message.pos, PipeLogisticsChassis.class);
        if (chassis == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final LogisticsModule module = chassis.getSubModule(message.slot);
        if (module instanceof IModuleMenuProvider) {
            IModuleMenuProvider.open(player, module);
        } else if (module instanceof LegacyModuleGui legacy) {
            // Not moved to a menu yet.
            LegacyModuleGui.getPipeGuiProvider(legacy).setTilePos(chassis.container).open(player);
        }
    }
}
