package logisticspipes.network.to_server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.ModuleTarget;

/**
 * The client's HUD subscribes to a module, or lets go of it.
 *
 * <p>Start and stop are one message with a flag rather than two types: they carry the same
 * addressing, they are always sent from the same pair of methods, and the receiver dispatches on
 * nothing else.
 */
public record ModuleWatchMessage(ModuleTarget target, boolean watching) implements CustomPacketPayload {

    public static final Type<ModuleWatchMessage> TYPE = new Type<>(LPConstants.rl("module_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleWatchMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ModuleWatchMessage::target,
                    ByteBufCodecs.BOOL, ModuleWatchMessage::watching,
                    ModuleWatchMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModuleWatchMessage message, IPayloadContext context) {
        final IModuleWatchReciver module =
                message.target.resolve(context.player(), IModuleWatchReciver.class);
        if (module == null) {
            return;
        }
        if (message.watching) {
            module.startWatching(context.player());
        } else {
            module.stopWatching(context.player());
        }
    }
}
