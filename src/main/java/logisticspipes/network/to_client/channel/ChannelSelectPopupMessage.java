package logisticspipes.network.to_client.channel;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.popup.GuiSelectChannelPopup;
import logisticspipes.network.to_server.pipe.SetInvSysConChannelMessage;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.ISubGuiController;

/**
 * The channels a player may pick from, opening the picker over the connector's screen.
 *
 * <p>The list is the server's answer to {@link
 * logisticspipes.network.to_server.channel.RequestChannelSelectMessage}: which channels a player
 * is allowed to see is not something the client can work out.
 */
public record ChannelSelectPopupMessage(BlockPos pos, List<ChannelInformation> channels)
        implements CustomPacketPayload {

    public static final Type<ChannelSelectPopupMessage> TYPE =
            new Type<>(LPConstants.rl("channel_select_popup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChannelSelectPopupMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChannelSelectPopupMessage::pos,
                    ChannelInformation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                            .apply(ByteBufCodecs.list()), ChannelSelectPopupMessage::channels,
                    ChannelSelectPopupMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChannelSelectPopupMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ISubGuiController controller) {
            controller.setSubGui(new GuiSelectChannelPopup(message.channels, message.pos,
                    selected -> ClientPacketDistributor.sendToServer(
                            new SetInvSysConChannelMessage(message.pos, selected.getChannelIdentifier()))));
        }
    }
}
