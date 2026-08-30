package logisticspipes.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.connection.ConnectionType;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.handler.codec.DecoderException;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.IReadListObject;
import logisticspipes.network.IWriteListObject;
import logisticspipes.proxy.LPRegistries;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public final class LPDataIOWrapper implements LPDataInput, LPDataOutput {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    /**
     * Cap on the initial capacity of a collection sized from the wire. The length prefix is
     * attacker-controlled, so pre-sizing to it lets a single small packet claiming
     * {@code Integer.MAX_VALUE} entries exhaust the heap before a single element is read.
     * Collections grow on demand, so a bounded initial capacity only costs a few resizes on
     * genuinely large payloads, and the element reads hit the end of the buffer long before
     * memory becomes a problem.
     */
    private static final int MAX_INITIAL_CAPACITY = 1024;
    private static final HashMap<Long, LPDataIOWrapper> BUFFER_WRAPPER_MAP = new HashMap<>();
    ByteBuf localBuffer;
    private int reference;
    /**
     * Registry access for the data-component codecs, which cannot encode values referencing a
     * datapack registry without it. Held per buffer rather than passed per call: it is a property
     * of the connection, and adding a parameter to the ~40 LPDataInput/LPDataOutput methods would
     * break the method references the packet classes use. May be null when the caller had none in
     * hand; {@link #registryBuf()} then falls back to {@link LPRegistries}.
     */
    @Nullable
    private RegistryAccess registryAccess;

    private LPDataIOWrapper(ByteBuf buffer) {
        localBuffer = buffer;
    }

    private static LPDataIOWrapper getInstance(ByteBuf buffer, @Nullable RegistryAccess registryAccess) {
        if (buffer.hasMemoryAddress()) {
            synchronized (BUFFER_WRAPPER_MAP) {
                LPDataIOWrapper instance = BUFFER_WRAPPER_MAP.get(buffer.memoryAddress());
                if (instance == null) {
                    instance = new LPDataIOWrapper(buffer);
                    BUFFER_WRAPPER_MAP.put(buffer.memoryAddress(), instance);
                }
                // Always overwrite: wrappers are pooled by memory address and reused, so a stale
                // registry access would otherwise leak into the next packet on that buffer.
                instance.registryAccess = registryAccess;
                ++instance.reference;
                return instance;
            }
        } else {
            LPDataIOWrapper instance = new LPDataIOWrapper(buffer);
            instance.registryAccess = registryAccess;
            return instance;
        }
    }

    public static void provideData(byte[] data, LPDataInputConsumer dataInputConsumer) {
        provideData(data, null, dataInputConsumer);
    }

    public static void provideData(byte[] data, @Nullable RegistryAccess registryAccess,
        LPDataInputConsumer dataInputConsumer) {
        ByteBuf dataBuffer = wrappedBuffer(data);
        LPDataIOWrapper lpData = getInstance(dataBuffer, registryAccess);

        dataInputConsumer.accept(lpData);

        lpData.unsetBuffer();
        dataBuffer.release();
    }

    public static byte[] collectData(LPDataOutputConsumer dataOutputConsumer) {
        return collectData(null, dataOutputConsumer);
    }

    public static byte[] collectData(@Nullable RegistryAccess registryAccess, LPDataOutputConsumer dataOutputConsumer) {
        ByteBuf dataBuffer = buffer();
        LPDataIOWrapper lpData = getInstance(dataBuffer, registryAccess);

        dataOutputConsumer.accept(lpData);

        lpData.unsetBuffer();

        byte[] data = new byte[dataBuffer.readableBytes()];
        dataBuffer.getBytes(0, data);

        dataBuffer.release();
        return data;
    }

    public static void provideData(ByteBuf dataBuffer, LPDataInputConsumer dataInputConsumer) {
        provideData(dataBuffer, null, dataInputConsumer);
    }

    public static void provideData(ByteBuf dataBuffer, @Nullable RegistryAccess registryAccess,
        LPDataInputConsumer dataInputConsumer) {
        // ignore empty data
        if (dataBuffer.readableBytes() == 0) {
            return;
        }
        LPDataIOWrapper lpData = getInstance(dataBuffer, registryAccess);

        dataInputConsumer.accept(lpData);

        lpData.unsetBuffer();
    }

    public static void writeData(ByteBuf dataBuffer, LPDataOutputConsumer dataOutputConsumer) {
        writeData(dataBuffer, null, dataOutputConsumer);
    }

    public static void writeData(ByteBuf dataBuffer, @Nullable RegistryAccess registryAccess,
        LPDataOutputConsumer dataOutputConsumer) {
        // ignore unwritable data
        if (dataBuffer.writableBytes() == 0) {
            return;
        }
        LPDataIOWrapper lpData = getInstance(dataBuffer, registryAccess);

        dataOutputConsumer.accept(lpData);

        lpData.unsetBuffer();
    }

    /**
     * Wraps (does not copy) the current buffer, so encoding into the result writes straight through.
     * <p>
     * Only for the codec-based values (item stacks, fluid stacks, component patches), which cannot
     * be encoded without a registry. Everything else must go through {@link #buf()}: resolving a
     * {@link RegistryAccess} throws when neither a server nor a client connection exists, which is
     * the normal situation in unit tests.
     */
    private RegistryFriendlyByteBuf registryBuf() {
        RegistryAccess access = registryAccess != null ? registryAccess : LPRegistries.access();
        return new RegistryFriendlyByteBuf(localBuffer, access, ConnectionType.OTHER);
    }

    /**
     * Wraps (does not copy) the current buffer, sharing its reader and writer indices, so vanilla's
     * readers and writers interleave freely with the direct {@link #localBuffer} access below.
     */
    private FriendlyByteBuf buf() {
        return new FriendlyByteBuf(localBuffer);
    }

    private void unsetBuffer() {
        if (localBuffer.hasMemoryAddress()) {
            synchronized (BUFFER_WRAPPER_MAP) {
                if (--reference < 1) {
                    BUFFER_WRAPPER_MAP.remove(localBuffer.memoryAddress());
                }
            }
        }
        localBuffer = null;
    }

    @Override
    public void writeByteArray(byte @Nullable [] arr) {
        if (arr == null) {
            writeInt(-1);
        } else {
            writeInt(arr.length);
            writeBytes(arr);
        }
    }

    @Override
    public byte @Nullable [] readByteArray() {
        final int length = readLengthPrefix(Byte.BYTES);
        if (length == -1) {
            return null;
        }

        return readBytes(length);
    }

    @Override
    public void writeByte(int b) {
        localBuffer.writeByte(b);
    }

    @Override
    public void writeByte(byte b) {
        localBuffer.writeByte(b);
    }

    @Override
    public void writeShort(int s) {
        localBuffer.writeShort(s);
    }

    @Override
    public void writeShort(short b) {
        localBuffer.writeShort(b);
    }

    @Override
    public void writeInt(int i) {
        localBuffer.writeInt(i);
    }

    @Override
    public void writeLong(long l) {
        localBuffer.writeLong(l);
    }

    @Override
    public void writeFloat(float f) {
        localBuffer.writeFloat(f);
    }

    @Override
    public void writeDouble(double d) {
        localBuffer.writeDouble(d);
    }

    @Override
    public void writeBoolean(boolean b) {
        localBuffer.writeBoolean(b);
    }

    @Override
    public void writeUTF(@Nullable String s) {
        if (s == null) {
            writeInt(-1);
        } else {
            writeByteArray(s.getBytes(UTF_8));
        }
    }

    @Override
    public void writeFacing(@Nullable Direction direction) {
        if (direction == null) {
            writeByte(Byte.MIN_VALUE);
        } else {
            writeByte(direction.ordinal());
        }
    }

    @Override
    public void writeIdentifier(@Nullable Identifier resource) {
        buf().writeNullable(resource, Identifier.STREAM_CODEC);
    }

    @Override
    public <T extends Enum<T>> void writeEnumSet(EnumSet<T> types, Class<T> clazz) {
        buf().writeEnumSet(types, clazz);
    }

    @Override
    public void writeBitSet(BitSet bits) {
        buf().writeBitSet(bits);
    }

    @Override
    public void writeCompoundTag(@Nullable CompoundTag tag) {
        // Vanilla frames null as a TAG_END marker and writes the tag uncompressed; the gzip round
        // trip this used to do bought nothing on a packet buffer.
        buf().writeNbt(tag);
    }

    @Override
    public void writeBooleanArray(boolean @Nullable [] arr) {
        if (arr == null) {
            writeInt(-1);
        } else if (arr.length == 0) {
            writeInt(0);
            writeByteArray(null);
        } else {
            BitSet bits = new BitSet(arr.length);
            for (int i = 0; i < arr.length; i++) {
                bits.set(i, arr[i]);
            }
            writeInt(arr.length);
            writeByteArray(bits.toByteArray());
        }
    }

    @Override
    public void writeUTFArray(@Nullable String @Nullable [] arr) {
        if (arr == null) {
            writeInt(-1);
        } else {
            writeInt(arr.length);
            for (String s : arr) {
                writeUTF(s);
            }
        }
    }

    @Override
    public void writeIntArray(int @Nullable [] arr) {
        if (arr == null) {
            writeInt(-1);
        } else {
            writeInt(arr.length);
            for (int i : arr) {
                writeInt(i);
            }
        }
    }

    @Override
    public void writeItemStack(ItemStack itemstack) {
        // Vanilla's codec already frames the empty stack and carries the count and every component,
        // which is what the hand-rolled framing here used to do badly: the damage was written but
        // never applied on read.
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf(), itemstack);
    }

    @Override
    public void writeFluidStack(FluidStack fluidstack) {
        FluidStack.OPTIONAL_STREAM_CODEC.encode(registryBuf(), fluidstack);
    }

    @Override
    public void writeItemIdentifier(@Nullable ItemIdentifier item) {
        // An explicit null marker rather than the old `itemId == 0` sentinel, which was
        // indistinguishable from minecraft:air.
        //
        // The static overload, not the instance one: that one pins the encoder to
        // `? super FriendlyByteBuf`, which a registry-aware codec is not.
        FriendlyByteBuf.writeNullable(registryBuf(), item, ItemIdentifier.STREAM_CODEC);
    }

    @Override
    public void writeItemIdentifierStack(@Nullable ItemIdentifierStack stack) {
        FriendlyByteBuf.writeNullable(registryBuf(), stack, ItemIdentifierStack.STREAM_CODEC);
    }

    @Override
    public <T> void writeCollection(@Nullable Collection<T> collection, IWriteListObject<T> handler) {
        if (collection == null) {
            writeInt(-1);
        } else {
            writeInt(collection.size());
            for (T obj : collection) {
                handler.writeObject(this, obj);
            }
        }
    }

    @Override
    public <T extends Enum<T>> void writeEnum(T obj) {
        writeInt(obj.ordinal());
    }

    @Override
    public void writeByteBuf(ByteBuf otherBuffer) {
        writeInt(otherBuffer.readableBytes());
        localBuffer.writeBytes(otherBuffer, otherBuffer.readableBytes());
    }

    @Override
    public void writeLongArray(long @Nullable [] arr) {
        if (arr == null) {
            writeInt(-1);
        } else {
            writeInt(arr.length);
            for (long l : arr) {
                writeLong(l);
            }
        }
    }

    @Override
    public void writeBytes(byte[] arr) {
        localBuffer.writeBytes(arr);
    }

    @Override
    public void writeChannelInformation(ChannelInformation channel) {
        ChannelInformation.STREAM_CODEC.encode(localBuffer, channel);
    }

    @Override
    public void writeUUID(@Nullable UUID uuid) {
        buf().writeNullable(uuid, UUIDUtil.STREAM_CODEC);
    }

    @Override
    public void writePlayerIdentifier(PlayerIdentifier playerIdentifier) {
        PlayerIdentifier.STREAM_CODEC.encode(localBuffer, playerIdentifier);
    }

    @Override
    public byte readByte() {
        return localBuffer.readByte();
    }

    @Override
    public short readShort() {
        return localBuffer.readShort();
    }

    @Override
    public int readInt() {
        return localBuffer.readInt();
    }

    @Override
    public long readLong() {
        return localBuffer.readLong();
    }

    @Override
    public float readFloat() {
        return localBuffer.readFloat();
    }

    @Override
    public double readDouble() {
        return localBuffer.readDouble();
    }

    @Override
    public boolean readBoolean() {
        return localBuffer.readBoolean();
    }

    @Nullable
    @Override
    public String readUTF() {
        byte[] arr = readByteArray();
        if (arr == null) {
            return null;
        } else {
            return new String(arr, UTF_8);
        }
    }

    @Nullable
    @Override
    public Direction readFacing() {
        byte b = localBuffer.readByte();

        if (b == Byte.MIN_VALUE) {
            return null;
        } else if (b < 0 || b >= Direction.values().length) {
            throw new IndexOutOfBoundsException("Invalid value for Direction");
        }
        return Direction.values()[b];
    }

    @Nullable
    @Override
    public Identifier readIdentifier() {
        return buf().readNullable(Identifier.STREAM_CODEC);
    }

    @Override
    public <T extends Enum<T>> EnumSet<T> readEnumSet(Class<T> clazz) {
        return buf().readEnumSet(clazz);
    }

    @Override
    public BitSet readBitSet() {
        return buf().readBitSet();
    }

    @Nullable
    @Override
    public CompoundTag readCompoundTag() {
        return buf().readNbt();
    }

    /**
     * Reads a length prefix for an allocation that must be exactly that size, so it cannot be
     * bounded by {@link #MAX_INITIAL_CAPACITY} the way a growable collection can. Every element
     * needs at least {@code minBytesPerElement} bytes on the wire, which gives a hard upper bound
     * from what is actually left in the buffer.
     *
     * @return the length, or -1 for the null marker
     */
    private int readLengthPrefix(int minBytesPerElement) {
        final int length = localBuffer.readInt();
        if (length == -1) {
            return -1;
        }
        if (length < 0) {
            throw new DecoderException("Negative length prefix: " + length);
        }
        final long required = (long) length * minBytesPerElement;
        if (required > localBuffer.readableBytes()) {
            throw new DecoderException("Length prefix of " + length + " needs at least " + required
                + " bytes, but only " + localBuffer.readableBytes() + " are readable");
        }
        return length;
    }

    /**
     * As {@link #readLengthPrefix}, for a collection that is filled element by element.
     */
    private int readSizePrefix() {
        final int size = readInt();
        if (size < -1) {
            throw new DecoderException("Negative size prefix: " + size);
        }
        return size;
    }

    @Override
    public boolean @Nullable [] readBooleanArray() {
        final int bitCount = localBuffer.readInt();
        if (bitCount == -1) {
            return null;
        }
        // The bits arrive packed into the byte array read below, so eight of them share one byte.
        if (bitCount < 0 || bitCount > (long) localBuffer.readableBytes() * Byte.SIZE) {
            throw new DecoderException("Bit count of " + bitCount + " exceeds the "
                + localBuffer.readableBytes() + " readable bytes");
        }

        byte[] data = readByteArray();
        if (bitCount == 0) {
            return new boolean[0];
        } else if (data == null) {
            throw new NullPointerException("Boolean's byte array is null");
        }

        BitSet bits = BitSet.valueOf(data);

        final boolean[] arr = new boolean[bitCount];
        IntStream.range(0, bitCount).forEach(i -> arr[i] = bits.get(i));
        return arr;
    }

    @Override
    public @Nullable String @Nullable [] readUTFArray() {
        // Each entry is a byte array, so it carries at least its own int length prefix.
        final int length = readLengthPrefix(Integer.BYTES);
        if (length == -1) {
            return null;
        }

        final String[] arr = new String[length];
        IntStream.range(0, length).forEach(i -> arr[i] = readUTF());
        return arr;
    }

    @Override
    public int @Nullable [] readIntArray() {
        final int length = readLengthPrefix(Integer.BYTES);
        if (length == -1) {
            return null;
        }

        final int[] arr = new int[length];
        IntStream.range(0, length).forEach(i -> arr[i] = localBuffer.readInt());
        return arr;
    }

    @Override
    public byte[] readBytes(int length) {
        if (length < 0 || length > localBuffer.readableBytes()) {
            throw new DecoderException("Cannot read " + length + " bytes, "
                + localBuffer.readableBytes() + " readable");
        }
        byte[] arr = new byte[length];
        localBuffer.readBytes(arr, 0, length);
        return arr;
    }

    @Nullable
    @Override
    public ItemIdentifier readItemIdentifier() {
        return FriendlyByteBuf.readNullable(registryBuf(), ItemIdentifier.STREAM_CODEC);
    }

    @Nullable
    @Override
    public ItemIdentifierStack readItemIdentifierStack() {
        return FriendlyByteBuf.readNullable(registryBuf(), ItemIdentifierStack.STREAM_CODEC);
    }

    @Override
    public ItemStack readItemStack() {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf());
    }

    @Override
    public FluidStack readFluidStack() {
        return FluidStack.OPTIONAL_STREAM_CODEC.decode(registryBuf());
    }

    @Nullable
    @Override
    public <T> ArrayList<T> readArrayList(IReadListObject<T> reader) {
        int size = readSizePrefix();
        if (size == -1) {
            return null;
        }

        ArrayList<T> list = new ArrayList<>(Math.min(size, MAX_INITIAL_CAPACITY));
        for (int i = 0; i < size; i++) {
            list.add(reader.readObject(this));
        }
        return list;
    }

    @Nullable
    @Override
    public <T> LinkedList<T> readLinkedList(IReadListObject<T> reader) {
        int size = readSizePrefix();
        if (size == -1) {
            return null;
        }

        LinkedList<T> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            list.add(reader.readObject(this));
        }
        return list;
    }

    @Nullable
    @Override
    public <T> Set<T> readSet(IReadListObject<T> handler) {
        int size = readSizePrefix();
        if (size == -1) {
            return null;
        }

        Set<T> set = new HashSet<>(Math.min(size, MAX_INITIAL_CAPACITY));
        for (int i = 0; i < size; i++) {
            set.add(handler.readObject(this));
        }
        return set;
    }

    @Nullable
    @Override
    public <T> NonNullList<T> readNonNullList(IReadListObject<T> reader, T fillItem) {
        // withSize allocates the full length up front, so this needs the hard bound rather than
        // a capped initial capacity. Every element costs at least one byte on the wire.
        int size = readLengthPrefix(Byte.BYTES);
        if (size == -1) {
            return null;
        }

        NonNullList<T> list = NonNullList.withSize(size, fillItem);
        for (int i = 0; i < size; i++) {
            T obj = reader.readObject(this);
            if (obj != null) {
                list.set(i, obj);
            }
        }
        return list;
    }

    @Nullable
    @Override
    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[localBuffer.readInt()];
    }

    @Override
    public ByteBuf readByteBuf() {
        byte[] arr = readByteArray();
        if (arr == null) {
            throw new NullPointerException("Buffer may not be null, but read null");
        } else {
            return wrappedBuffer(arr);
        }
    }

    @Override
    public long @Nullable [] readLongArray() {
        final int length = readLengthPrefix(Long.BYTES);
        if (length == -1) {
            return null;
        }

        final long[] arr = new long[length];
        IntStream.range(0, length).forEach(i -> arr[i] = localBuffer.readLong());
        return arr;
    }

    @Override
    public ChannelInformation readChannelInformation() {
        return ChannelInformation.STREAM_CODEC.decode(localBuffer);
    }

    @Nullable
    @Override
    public UUID readUUID() {
        return buf().readNullable(UUIDUtil.STREAM_CODEC);
    }

    @Override
    public PlayerIdentifier readPlayerIdentifier() {
        return PlayerIdentifier.STREAM_CODEC.decode(localBuffer);
    }

}
