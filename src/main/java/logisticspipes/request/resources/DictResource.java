package logisticspipes.request.resources;

import java.util.BitSet;
import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.FuzzyFlag;
import logisticspipes.utils.FuzzyUtil;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;

public class DictResource implements IResource {

    /**
     * Null on the client, which only ever displays a resource.
     */
    private final @Nullable IRequestItems requester;
    public ItemIdentifierStack stack;
    private final BitSet fuzzyFlags = new BitSet(4);

    /**
     * The four fuzzy flags are named rather than sent as the {@link BitSet} that holds them.
     *
     * <p>The old encoding wrote {@code fuzzyFlags.get(0, 3)}, which silently dropped
     * {@link FuzzyFlag#USE_ORE_CATEGORY} -- a leftover from when there were three flags. Naming
     * them makes a fourth one impossible to forget, and leaves nothing to read a bit index from.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DictResource> STREAM_CODEC =
        StreamCodec.composite(
            ItemIdentifierStack.STREAM_CODEC, resource -> resource.stack,
            ByteBufCodecs.BOOL, resource -> resource.hasFuzzyFlag(FuzzyFlag.USE_ORE_DICT),
            ByteBufCodecs.BOOL, resource -> resource.hasFuzzyFlag(FuzzyFlag.IGNORE_DAMAGE),
            ByteBufCodecs.BOOL, resource -> resource.hasFuzzyFlag(FuzzyFlag.IGNORE_NBT),
            ByteBufCodecs.BOOL, resource -> resource.hasFuzzyFlag(FuzzyFlag.USE_ORE_CATEGORY),
            DictResource::new);

    /**
     * Rebuilt on the client, where a resource has no requester to answer to.
     */
    private DictResource(ItemIdentifierStack stack, boolean useOreDict, boolean ignoreDamage,
        boolean ignoreNbt, boolean useOreCategory) {
        this(stack, null);
        setFuzzyFlag(FuzzyFlag.USE_ORE_DICT, useOreDict);
        setFuzzyFlag(FuzzyFlag.IGNORE_DAMAGE, ignoreDamage);
        setFuzzyFlag(FuzzyFlag.IGNORE_NBT, ignoreNbt);
        setFuzzyFlag(FuzzyFlag.USE_ORE_CATEGORY, useOreCategory);
    }

    public boolean hasFuzzyFlag(FuzzyFlag flag) {
        return FuzzyUtil.get(fuzzyFlags, flag);
    }

    private void setFuzzyFlag(FuzzyFlag flag, boolean value) {
        FuzzyUtil.set(fuzzyFlags, flag, value);
    }

    public DictResource(ItemIdentifierStack stack, @Nullable IRequestItems requester) {
        this.stack = stack;
        this.requester = requester;
    }

    @Override
    public ItemIdentifier getAsItem() {
        return stack.getItem();
    }

    @Override
    public int getRequestedAmount() {
        return stack.getStackSize();
    }

    @Override
    public boolean matches(IResource resource, MatchSettings settings) {
        if (resource instanceof DictResource dict) {
            return matches(dict.getItem(), MatchSettings.NORMAL)
                && dict.matches(getItem(), MatchSettings.NORMAL)
                && fuzzyFlags.equals(dict.fuzzyFlags);
        } else if (resource instanceof ItemResource item) {
            return matches(item.getItem(), MatchSettings.NORMAL);
        }
        return false;
    }

    @Override
    public boolean matches(ItemIdentifier other, MatchSettings settings) {
        return FuzzyUtil.fuzzyMatches(FuzzyUtil.getter(fuzzyFlags), stack.getItem(), other);
    }

    @Override
    public IRouter getRouter() {
        return Objects.requireNonNull(requester, "a client-side resource has no router").getRouter();
    }

    @Override
    public IResource clone(int multiplier) {
        ItemIdentifierStack stack = new ItemIdentifierStack(this.stack);
        stack.setStackSize(stack.getStackSize() * multiplier);
        DictResource clone = new DictResource(stack, requester);
        clone.fuzzyFlags.or(fuzzyFlags);
        return clone;
    }

    public DictResource copy() {
        DictResource clone = new DictResource(new ItemIdentifierStack(this.stack), requester);
        clone.fuzzyFlags.or(fuzzyFlags);
        return clone;
    }

    public @Nullable IRequestItems getTarget() {
        return requester;
    }

    public ItemIdentifier getItem() {
        return stack.getItem();
    }

    public ItemIdentifierStack getItemStack() {
        return stack;
    }

    @Override
    public boolean mergeForDisplay(IResource resource, int withAmount) {
        if (resource instanceof DictResource dict && dict.fuzzyFlags.equals(fuzzyFlags)
            && dict.getItem().equals(getItem())) {
            stack.setStackSize(stack.getStackSize() + withAmount);
            return true;
        }
        return false;
    }

    @Override
    public IResource copyForDisplayWith(int amount) {
        ItemIdentifierStack stack = new ItemIdentifierStack(this.stack);
        stack.setStackSize(amount);
        DictResource clone = new DictResource(stack, null);
        clone.fuzzyFlags.or(fuzzyFlags);
        return clone;
    }

    @Override
    public String getDisplayText(ColorCode code) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GRAY);
        builder.append("{");
        if (code != ColorCode.NONE) {
            builder.append(code == ColorCode.MISSING ? ChatColor.RED : ChatColor.GREEN);
        }
        builder.append(stack.getFriendlyName());
        if (code != ColorCode.NONE) {
            builder.append(ChatColor.GRAY);
        }
        builder.append(" [");
        builder.append(useOreDict() ? ChatColor.GREEN : ChatColor.RED);
        builder.append("OreDict");
        builder.append(ChatColor.GRAY);
        builder.append(", ");
        builder.append(useOreCategory() ? ChatColor.GREEN : ChatColor.RED);
        builder.append("OreCat");
        builder.append(ChatColor.GRAY);
        builder.append(", ");
        builder.append(ignoreDamage() ? ChatColor.GREEN : ChatColor.RED);
        builder.append("IgnDmg");
        builder.append(ChatColor.GRAY);
        builder.append(", ");
        builder.append(ignoreNBT() ? ChatColor.GREEN : ChatColor.RED);
        builder.append("IgnNBT");
        builder.append(ChatColor.GRAY);
        return builder.append("]}").toString();
    }

    @Override
    public ItemIdentifierStack getDisplayItem() {
        return stack;
    }

    public DictResource loadFromBitSet(BitSet bits) {
        fuzzyFlags.clear();
        fuzzyFlags.or(bits);
        return this;
    }

    /**
     * A copy of all four fuzzy flags.
     */
    public BitSet getBitSet() {
        return (BitSet) fuzzyFlags.clone();
    }

    public Identifier getIdentifier() {
        return new Identifier();
    }

    public boolean useOreDict() {
        return FuzzyUtil.get(fuzzyFlags, FuzzyFlag.USE_ORE_DICT);
    }

    public boolean useOreCategory() {
        return FuzzyUtil.get(fuzzyFlags, FuzzyFlag.USE_ORE_CATEGORY);
    }

    public boolean ignoreDamage() {
        return FuzzyUtil.get(fuzzyFlags, FuzzyFlag.IGNORE_DAMAGE);
    }

    public boolean ignoreNBT() {
        return FuzzyUtil.get(fuzzyFlags, FuzzyFlag.IGNORE_NBT);
    }

    public class Identifier {

        private ItemIdentifier getItem() {
            return stack.getItem();
        }

        private BitSet getFlags() {
            return fuzzyFlags;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getItem(), getFlags());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Identifier id && id.getItem().equals(getItem()) && id.getFlags().equals(getFlags());
        }
    }

}
