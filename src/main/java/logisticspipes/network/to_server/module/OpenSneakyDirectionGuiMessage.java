package logisticspipes.network.to_server.module;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.module.inhand.SneakyModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inpipe.SneakyModuleInSlotGuiProvider;
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
        if (module == null) {
            return;
        }
        if (message.target.slot().orElse(null) == ModulePositionType.IN_HAND) {
            // The module is in a dummy container, which the picker replaces.
            context.player().closeContainer();
            NewGuiHandler.getGui(SneakyModuleInHandGuiProvider.class)
                    .setInvSlot(message.target.positionInt())
                    .open(context.player());
            return;
        }
        NewGuiHandler.getGui(SneakyModuleInSlotGuiProvider.class)
                .setSneakyOrientation(module.getSneakyDirection())
                .setSlot(message.target.slot().orElse(null))
                .setPositionInt(message.target.positionInt())
                .setPosX(message.target.pos().getX())
                .setPosY(message.target.pos().getY())
                .setPosZ(message.target.pos().getZ())
                .open(context.player());
    }
}
