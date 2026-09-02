package logisticspipes.network.to_client.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.gui.orderer.GuiOrderer;

/**
 * Which dimension the pipe a remote orderer is bound to lives in.
 *
 * <p>It arrives just before the screen opens, so when there is no screen yet the value is cached
 * for the one about to be built.
 */
public record RemoteOrdererDimensionMessage(Identifier dimension) implements CustomPacketPayload {

    public static final Type<RemoteOrdererDimensionMessage> TYPE =
            new Type<>(LPConstants.rl("remote_orderer_dimension"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteOrdererDimensionMessage> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, RemoteOrdererDimensionMessage::dimension,
                    RemoteOrdererDimensionMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoteOrdererDimensionMessage message, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof GuiOrderer gui) {
            gui.dimension = message.dimension;
            gui.refreshItems();
        } else {
            GuiOrderer.dimensioncache = message.dimension;
            GuiOrderer.cachetime = System.currentTimeMillis();
        }
    }
}
