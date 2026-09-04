package logisticspipes.network.to_client.channel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IGUIChannelInformationReceiver;
import logisticspipes.routing.channels.ChannelInformation;

/**
 * One channel's details, for whichever screen is showing channels.
 *
 * <p>Addressed to the open screen rather than to a position: the same channel is shown by the
 * connector's screen, by the channel manager popup over the security station, and by the popup
 * that picks a channel. A screen that is no longer open simply drops it.
 *
 * @param targeted true when this answers something the screen asked for, false when it is the
 *                 channel manager telling every watcher that a channel changed
 */
public record ChannelInformationMessage(ChannelInformation channel, boolean targeted)
        implements CustomPacketPayload {

    public static final Type<ChannelInformationMessage> TYPE =
            new Type<>(LPConstants.rl("channel_information"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChannelInformationMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ChannelInformation.STREAM_CODEC.cast(), ChannelInformationMessage::channel,
                    ByteBufCodecs.BOOL, ChannelInformationMessage::targeted,
                    ChannelInformationMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChannelInformationMessage message, IPayloadContext context) {
        final Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof IGUIChannelInformationReceiver receiver) {
            receiver.handleChannelInformation(message.channel, message.targeted);
        }
    }
}
