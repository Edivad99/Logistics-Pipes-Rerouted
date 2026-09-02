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
 * How a request went.
 *
 * <p>Goes to the screen that asked when one is open and the player wants pop-ups, and to the chat
 * otherwise -- the request may well have been made from a remote orderer that has since been
 * closed.
 *
 * @param missing whether the listed resources are what could not be found, rather than what was
 *                successfully requested
 */
public record RequestAnswerMessage(List<IResource> resources, boolean missing) implements CustomPacketPayload {

    public static final Type<RequestAnswerMessage> TYPE = new Type<>(LPConstants.rl("request_answer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAnswerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    IResource.STREAM_CODEC.apply(ByteBufCodecs.list()), RequestAnswerMessage::resources,
                    ByteBufCodecs.BOOL, RequestAnswerMessage::missing,
                    RequestAnswerMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestAnswerMessage message, IPayloadContext context) {
        final Player player = context.player();
        final var screen = Minecraft.getInstance().screen;
        if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && screen instanceof GuiOrderer gui) {
            gui.handleRequestAnswer(message.resources, message.missing, gui, player);
        } else if (LPConfigs.COMMON.DISPLAY_POPUP.getAsBoolean() && screen instanceof GuiRequestTable gui) {
            gui.handleRequestAnswer(message.resources, message.missing, gui, player);
        } else if (message.missing) {
            for (IResource resource : message.resources) {
                player.sendSystemMessage(Component.literal("Missing: " + resource.getDisplayText(ColorCode.NONE))
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            for (IResource resource : message.resources) {
                player.sendSystemMessage(Component.literal("Requested: " + resource.getDisplayText(ColorCode.NONE))
                        .withStyle(ChatFormatting.GREEN));
            }
            player.sendSystemMessage(Component.literal("Request successful!").withStyle(ChatFormatting.GREEN));
        }
    }
}
