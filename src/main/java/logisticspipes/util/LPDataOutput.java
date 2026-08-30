package logisticspipes.util;

import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.IWriteListObject;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public interface LPDataOutput {

    /**
     * @see java.io.DataOutput#writeByte(int)
     */
    void writeByte(int b);

    void writeByte(byte b);

    /**
     * @see java.io.DataOutput#writeShort(int)
     */
    void writeShort(int s);

    void writeShort(short s);

    /**
     * @see java.io.DataOutput#writeInt(int)
     */
    void writeInt(int i);

    /**
     * @see java.io.DataOutput#writeLong(long)
     */
    void writeLong(long l);

    /**
     * @see java.io.DataOutput#writeFloat(float)
     */
    void writeFloat(float f);

    /**
     * @see java.io.DataOutput#writeDouble(double)
     */
    void writeDouble(double d);

    /**
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    void writeBoolean(boolean b);

    /**
     * Uses UTF-8 and not UTF-16.
     *
     * @see java.io.DataOutput#writeUTF(String)
     */
    void writeUTF(@Nullable String s);

    void writeByteArray(byte @Nullable [] arr);

    void writeByteBuf(ByteBuf buffer);

    void writeIntArray(int @Nullable [] arr);

    void writeLongArray(long @Nullable [] arr);

    void writeBooleanArray(boolean @Nullable [] arr);

    void writeUTFArray(@Nullable String @Nullable [] arr);

    void writeFacing(@Nullable Direction direction);

    void writeIdentifier(@Nullable Identifier resource);

    <T extends Enum<T>> void writeEnumSet(EnumSet<T> types, Class<T> clazz);

    void writeBitSet(BitSet bits);

    void writeCompoundTag(@Nullable CompoundTag tag);

    void writeItemStack(ItemStack itemstack);

    void writeFluidStack(FluidStack fluidstack);

    void writeItemIdentifier(@Nullable ItemIdentifier item);

    void writeItemIdentifierStack(@Nullable ItemIdentifierStack stack);

    <T> void writeCollection(@Nullable Collection<T> collection, IWriteListObject<T> handler);

    default <T extends LPFinalSerializable> void writeCollection(Collection<T> collection) {
        writeCollection(collection, LPDataOutput::writeSerializable);
    }

    <T extends Enum<T>> void writeEnum(T obj);

    void writeBytes(byte[] arr);

    void writeChannelInformation(ChannelInformation channel);

    void writeUUID(@Nullable UUID uuid);

    void writePlayerIdentifier(PlayerIdentifier playerIdentifier);

    default void writeSerializable(LPFinalSerializable finalSerializable) {
        finalSerializable.write(this);
    }

    interface LPDataOutputConsumer {

        void accept(LPDataOutput dataOutput);
    }
}
