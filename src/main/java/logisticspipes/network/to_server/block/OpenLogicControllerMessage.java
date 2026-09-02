package logisticspipes.network.to_server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.TargetLookup;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.inventory.LogicControllerMenu;

/**
 * Opens the logic controller layout of a pipe, from the button on its controller screen.
 *
 * <p>The menu is built here rather than by the block entity: a pipe's block entity hosts any kind
 * of pipe, and this is one screen among several it could show.
 */
public record OpenLogicControllerMessage(BlockPos pos) implements CustomPacketPayload {

    public static final Type<OpenLogicControllerMessage> TYPE =
            new Type<>(LPConstants.rl("open_logic_controller"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLogicControllerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenLogicControllerMessage::pos,
                    OpenLogicControllerMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenLogicControllerMessage message, IPayloadContext context) {
        final LogisticsTileGenericPipe container =
                TargetLookup.blockEntityAt(context.player(), message.pos, LogisticsTileGenericPipe.class);
        if (container == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, viewer) -> new LogicControllerMenu(containerId, inventory, container),
                        Component.empty()),
                buffer -> buffer.writeBlockPos(message.pos));
    }
}
