package logisticspipes.network.to_client.pipe;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.gui.popup.GuiSelectSatellitePopup;
import logisticspipes.pipes.SatelliteEntry;
import logisticspipes.utils.gui.ISubGuiController;

/**
 * The satellites the asking pipe can route to, nearest first.
 */
public record SatellitePipeListMessage(List<SatelliteEntry> satellites) implements CustomPacketPayload {

    public static final Type<SatellitePipeListMessage> TYPE = new Type<>(LPConstants.rl("satellite_pipe_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SatellitePipeListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    SatelliteEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SatellitePipeListMessage::satellites,
                    SatellitePipeListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SatellitePipeListMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ISubGuiController controller) {
            final GuiSelectSatellitePopup popup = controller.findSubGui(GuiSelectSatellitePopup.class);
            if (popup != null) {
                popup.handleSatelliteList(message.satellites);
            }
        }
    }
}
