package logisticspipes.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.IReadListObject;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public interface LPDataInput {

    byte @Nullable [] readByteArray();

    /**
     * @see java.io.DataInput#readByte()
     */
    byte readByte();

    /**
     * @see java.io.DataInput#readShort()
     */
    short readShort();

    /**
     * @see java.io.DataInput#readInt()
     */
    int readInt();

    /**
     * @see java.io.DataInput#readLong()
     */
    long readLong();

    /**
     * @see java.io.DataInput#readFloat()
     */
    float readFloat();

    /**
     * @see java.io.DataInput#readDouble()
     */
    double readDouble();

    /**
     * @see java.io.DataInput#readBoolean()
     */
    boolean readBoolean();

    /**
     * @see java.io.DataInput#readUTF()
     */
    @Nullable
    String readUTF();

    @Nullable
    Direction readFacing();

    @Nullable
    Identifier readIdentifier();

    <T extends Enum<T>> EnumSet<T> readEnumSet(Class<T> clazz);

    BitSet readBitSet();

    @Nullable
    CompoundTag readCompoundTag();

    boolean @Nullable [] readBooleanArray();

    @Nullable String @Nullable [] readUTFArray();

    int @Nullable [] readIntArray();

    byte[] readBytes(int length);

    @Nullable
    ItemIdentifier readItemIdentifier();

    @Nullable
    ItemIdentifierStack readItemIdentifierStack();

    ItemStack readItemStack();

    FluidStack readFluidStack();

    @Nullable
    <T> ArrayList<T> readArrayList(IReadListObject<T> reader);

    @Nullable
    <T> LinkedList<T> readLinkedList(IReadListObject<T> reader);

    @Nullable
    <T> Set<T> readSet(IReadListObject<T> handler);

    @Nullable
    <T> NonNullList<T> readNonNullList(IReadListObject<T> reader, T fillItem);

    @Nullable
    <T extends Enum<T>> T readEnum(Class<T> clazz);

    ByteBuf readByteBuf();

    long @Nullable [] readLongArray();

    ChannelInformation readChannelInformation();

    @Nullable
    UUID readUUID();

    PlayerIdentifier readPlayerIdentifier();

    default void readSerializable(LPSerializable serializable) {
        serializable.read(this);
    }

    interface LPDataInputConsumer {

        void accept(LPDataInput dataInput);
    }
}
