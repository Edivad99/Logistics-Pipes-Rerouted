/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;

public final class ItemIdentifierStack implements Comparable<ItemIdentifierStack> {

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemIdentifierStack> STREAM_CODEC =
        StreamCodec.composite(
            ItemIdentifier.STREAM_CODEC, ItemIdentifierStack::getItem,
            ByteBufCodecs.INT, ItemIdentifierStack::getStackSize,
            ItemIdentifierStack::new);

    /**
     * A list that may hold gaps, for contents addressed by position.
     *
     * <p>{@link #getListFromInventory(Container)} puts a null in for every empty slot, and the
     * receivers copy the list into their slots by index, so dropping the gaps would slide every
     * item into the wrong slot. A plain list codec cannot write a null element -- it throws inside
     * the encoder, which kills the connection rather than the packet -- so the gaps travel as an
     * empty optional.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<@Nullable ItemIdentifierStack>>
        NULLABLE_LIST_STREAM_CODEC = ByteBufCodecs.optional(STREAM_CODEC).apply(ByteBufCodecs.list())
            .map(
                optionals -> optionals.stream().map(item -> item.orElse(null)).toList(),
                items -> items.stream().map(Optional::ofNullable).toList());

    @Getter
    private final ItemIdentifier item;
    @Setter
    @Getter
    private int stackSize;

    public ItemIdentifierStack(ItemIdentifier item, int stackSize) {
        this.item = item;
        this.stackSize = stackSize;
    }

    public ItemIdentifierStack(ItemIdentifierStack copy) {
        this(copy.item, copy.stackSize);
    }

    public static ItemIdentifierStack getFromStack(ItemStack stack) {
        return new ItemIdentifierStack(ItemIdentifier.get(stack), stack.getCount());
    }

    /**
     * Reads back what {@link #saveToNBT} wrote, or null when the entry is unreadable.
     */
    @Nullable
    public static ItemIdentifierStack loadFromNBT(CompoundTag entry, HolderLookup.Provider provider) {
        if (!entry.contains("item")) {
            if (entry.contains("id")) {
                LogisticsPipes.LOG.warn(
                    "Skipping an item stored in the pre-DataComponents format (numeric item id + damage + nbt). "
                        + "It cannot be migrated automatically; re-save it to update the format.");
            }
            return null;
        }
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, entry.get("item"))
            .resultOrPartial(error -> LogisticsPipes.LOG.error("Could not read stored item: {}", error))
            .map(stack -> new ItemIdentifierStack(ItemIdentifier.get(stack), entry.getIntOr("amount", 0)))
            .orElse(null);
    }

    /** The inventory's contents by slot, with a null standing in for every empty slot. */
    public static LinkedList<@Nullable ItemIdentifierStack> getListFromInventory(Container inv) {
        return ItemIdentifierStack.getListFromInventory(inv, false);
    }

    public static LinkedList<@Nullable ItemIdentifierStack> getListFromInventory(Container inv, boolean removeNull) {
        LinkedList<@Nullable ItemIdentifierStack> list = new LinkedList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                if (!removeNull) {
                    list.add(null);
                }
            } else {
                list.add(ItemIdentifierStack.getFromStack(inv.getItem(i)));
            }
        }
        return list;
    }

    /**
     * Serializes this stack as <code>{ item: &lt;namespaced id + components&gt;, amount: int }</code>.
     * <p>
     * {@link ItemStack#CODEC} rather than {@link ItemStack#CODEC}: the latter caps the
     * count at 99, and the amounts stored here routinely exceed that, so the count is kept as a
     * separate field. Note that the codec drops transient components, matching vanilla's own
     * persistence behaviour.
     */
    public CompoundTag saveToNBT(HolderLookup.Provider provider) {
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag entry = new CompoundTag();
        entry.put("item", ItemStack.CODEC.encodeStart(ops, item.makeNormalStack(1)).getOrThrow());
        entry.putInt("amount", getStackSize());
        return entry;
    }

    public void lowerStackSize(int stackSize) {
        this.stackSize -= stackSize;
    }

    public ItemStack makeNormalStack() {
        return item.makeNormalStack(stackSize);
    }

    public ItemEntity makeEntityItem(Level level, BlockPos pos) {
        return item.makeEntityItem(stackSize, level, pos);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof ItemIdentifierStack stack) {
            return stack.item.equals(item) && stack.getStackSize() == getStackSize();
        }
        if ((that instanceof ItemIdentifier)) {
            throw new IllegalStateException(
                "Comparison between ItemIdentifierStack and ItemIdentifier -- did you forget a .getItem() in your code?");
        }

        return false;
    }

    @Override
    public int hashCode() {
        return item.hashCode() ^ (1023 * getStackSize());
    }

    @Override
    public String toString() {
        return String.format("%dx %s", getStackSize(), item);
    }

    public String getFriendlyName() {
        return getStackSize() + " " + item.getFriendlyName();
    }

    @Override
    public int compareTo(ItemIdentifierStack o) {
        int c = item.compareTo(o.item);
        if (c == 0) {
            return getStackSize() - o.getStackSize();
        }
        return c;
    }

}
