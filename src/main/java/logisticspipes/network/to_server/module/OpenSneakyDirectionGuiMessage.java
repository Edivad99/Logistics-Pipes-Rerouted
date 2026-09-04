package logisticspipes.network.to_server.module;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.network.ModuleTarget;
import logisticspipes.world.inventory.SneakyDirectionMenu;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;

/**
 * Opens the side-picker of an advanced extractor.
 *
 * <p>The module carries the side it currently extracts from, and only the server has the module,
 * so the screen is opened from here rather than by the button that was clicked.
 */
public record OpenSneakyDirectionGuiMessage(ModuleTarget target) implements CustomPacketPayload {

    public static final Type<OpenSneakyDirectionGuiMessage> TYPE =
            new Type<>(LPConstants.rl("open_sneaky_direction_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSneakyDirectionGuiMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, OpenSneakyDirectionGuiMessage::target,
                    OpenSneakyDirectionGuiMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenSneakyDirectionGuiMessage message, IPayloadContext context) {
        final AsyncAdvancedExtractor module =
                message.target.resolve(context.player(), AsyncAdvancedExtractor.class);
        if (module == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // Opened in place of the module's own screen, so the menu is named here rather than asked
        // of the module.
        player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, viewer) ->
                                new SneakyDirectionMenu(containerId, inventory, message.target, module),
                        Component.empty()),
                buffer -> {
                    ModuleTarget.STREAM_CODEC.encode(buffer, message.target);
                    ByteBufCodecs.optional(Direction.STREAM_CODEC)
                            .encode(buffer, Optional.ofNullable(module.getSneakyDirection()));
                });
    }
}
