package logisticspipes.network;

import io.netty.buffer.Unpooled;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

/**
 * Carries one of our messages inside a block entity's update tag.
 *
 * <p>The initial state of a pipe reaches the client with the chunk rather than through the payload
 * channel, so it has to travel as NBT. Each block entity writes and reads its own message type, so
 * unlike a real payload this needs no name on the wire.
 *
 * <p>This rides {@code getUpdateTag}, which is chunk sync and never reaches disk, so the encoding
 * is free to change with the mod.
 */
public final class UpdateTagPayload {

    private static final String KEY = "LogisticsPipes:PayloadData";

    private UpdateTagPayload() {}

    public static <T> void write(CompoundTag nbt, StreamCodec<? super FriendlyByteBuf, T> codec, T message) {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buffer, message);
        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        nbt.putByteArray(KEY, bytes);
    }

    /**
     * @return the message, or null when the tag carries none -- a block entity saved before this
     *         key existed, or one whose update tag is not ours
     */
    public static <T> @Nullable T read(ValueInput input, StreamCodec<? super FriendlyByteBuf, T> codec) {
        // ValueInput has no byte-array accessor, so the bytes come back through Codec.BYTE_BUFFER,
        // which NbtOps maps onto the same ByteArrayTag the writer produces.
        final byte[] bytes = input.read(KEY, Codec.BYTE_BUFFER)
                .map(buffer -> {
                    byte[] copy = new byte[buffer.remaining()];
                    buffer.duplicate().get(copy);
                    return copy;
                })
                .orElse(new byte[0]);
        if (bytes.length == 0) {
            return null;
        }
        return codec.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
    }
}
