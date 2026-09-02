package logisticspipes.network.to_client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;
import logisticspipes.gui.orderer.GuiOrderer;
import logisticspipes.gui.orderer.GuiRequestTable;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.IResource.ColorCode;

/**
 * What a request would consume and what it would still be short of, without actually making it.
 *
 * <p>The answer to the request GUI's "simulate" button.
 */
public record RequestComponentsMessage(List<IResource> used, List<IResource> missing)
        implements CustomPacketPayload {

    public static final Type<RequestComponentsMessage> TYPE =
            new Type<>(LPConstants.rl("request_components"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestComponentsMessage> STREAM_CODEC =
            StreamCodec.composite(
                    IResource.STREAM_CODEC.apply(ByteBufCodecs.list()), RequestComponentsMessage::used,
                    IResource.STREAM_CODEC.apply(ByteBufCodecs.list()), RequestComponentsMessage::missing,
                    RequestComponentsMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestComponentsMessage message, IPayloadContext context) {
        final Player player = context.player();
        final var screen = Minecraft.getInstance().screen;
        if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && screen instanceof GuiOrderer gui) {
            gui.handleSimulateAnswer(message.used, message.missing, gui, player);
        } else if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && screen instanceof GuiRequestTable gui) {
            gui.handleSimulateAnswer(message.used, message.missing, gui, player);
        } else {
            for (IResource resource : message.used) {
                player.sendSystemMessage(Component.literal("Component: " + resource.getDisplayText(ColorCode.NONE))
                        .withStyle(ChatFormatting.GREEN));
            }
            for (IResource resource : message.missing) {
                player.sendSystemMessage(Component.literal("Missing: " + resource.getDisplayText(ColorCode.NONE))
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}
