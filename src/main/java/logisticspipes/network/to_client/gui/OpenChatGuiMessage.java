package logisticspipes.network.to_client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;

/**
 * Opens the chat, because the server has just asked the player a question there.
 *
 * <p>The mod's debug tools converse in chat: they print a yes/no question and wait for the answer,
 * so the chat has to be open for the player to give one.
 */
public record OpenChatGuiMessage() implements CustomPacketPayload {

    public static final Type<OpenChatGuiMessage> TYPE = new Type<>(LPConstants.rl("open_chat_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChatGuiMessage> STREAM_CODEC =
            StreamCodec.unit(new OpenChatGuiMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenChatGuiMessage message, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new ChatScreen("", false));
    }
}
