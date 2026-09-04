package logisticspipes.network.to_client.channel;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.client.gui.popup.GuiManageChannelPopup;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.gui.ISubGuiController;

/**
 * The channels a player may manage, opening the manager over the security station's screen.
 */
public record ChannelManagerPopupMessage(BlockPos pos, List<ChannelInformation> channels)
        implements CustomPacketPayload {

    public static final Type<ChannelManagerPopupMessage> TYPE =
            new Type<>(LPConstants.rl("channel_manager_popup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChannelManagerPopupMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChannelManagerPopupMessage::pos,
                    ChannelInformation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                            .apply(ByteBufCodecs.list()), ChannelManagerPopupMessage::channels,
                    ChannelManagerPopupMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChannelManagerPopupMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ISubGuiController controller) {
            controller.setSubGui(new GuiManageChannelPopup(message.channels, message.pos));
        }
    }
}
