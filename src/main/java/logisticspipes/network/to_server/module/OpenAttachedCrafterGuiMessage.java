package logisticspipes.network.to_server.module;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.ModuleTarget;

/**
 * Opens the screen of whatever inventory a crafting module is pointed at.
 *
 * <p>Only the server can say what that is, so the client asks rather than opening anything.
 */
public record OpenAttachedCrafterGuiMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<OpenAttachedCrafterGuiMessage> TYPE =
            new Type<>(LPConstants.rl("open_attached_crafter_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAttachedCrafterGuiMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, OpenAttachedCrafterGuiMessage::target,
                    OpenAttachedCrafterGuiMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAttachedCrafterGuiMessage message, IPayloadContext context) {
        final ModuleCrafter module = message.target.resolve(context.player(), ModuleCrafter.class);
        if (module != null) {
            module.openAttachedGui(context.player());
        }
    }
}
