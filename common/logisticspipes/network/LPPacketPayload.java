package logisticspipes.network;

import io.netty.buffer.Unpooled;
import logisticspipes.LPConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Single multiplexed payload type for all LogisticsPipes packets.
 * LP uses a custom short-discriminator binary format; rather than registering each of
 * its ~60 packet types separately, one payload wraps the full LP binary stream.
 *
 * Wire format (written by {@link PacketHandler#fillByteBuf}):
 *   short  — packet ID (index into PacketHandler.packetlist)
 *   int    — debug ID
 *   ...    — LPDataOutput-encoded packet body
 */
public final class LPPacketPayload implements CustomPacketPayload {

    public static final Type<LPPacketPayload> TYPE = new Type<>(LPConstants.rl("packet"));

    private final FriendlyByteBuf data;

    private LPPacketPayload(FriendlyByteBuf data) {
        this.data = data;
    }

    /**
     * Creates a payload for sending.
     */
    public static LPPacketPayload of(FriendlyByteBuf data) {
        return new LPPacketPayload(data);
    }

    /**
     * Decoder used by StreamCodec.
     */
    public static LPPacketPayload decode(RegistryFriendlyByteBuf buf) {
        FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(buf.readableBytes()));
        copy.writeBytes(buf);
        return new LPPacketPayload(copy);
    }

    /**
     * Encoder used by StreamCodec.
     */
    public static void encode(RegistryFriendlyByteBuf buf, LPPacketPayload payload) {
        buf.writeBytes(
                payload.data,
                payload.data.readerIndex(),
                payload.data.readableBytes()
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, LPPacketPayload> STREAM_CODEC =
            StreamCodec.of(
                    LPPacketPayload::encode,
                    LPPacketPayload::decode
            );

    public FriendlyByteBuf getData() {
        return data;
    }

    public void release() {
        if (data.refCnt() > 0) {
            data.release();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
