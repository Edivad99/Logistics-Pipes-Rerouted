/*
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import net.neoforged.fml.ModList;

import com.mojang.serialization.Codec;

import logisticspipes.LogisticsPipes;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.WeakInternCache;
import net.minecraft.resources.ResourceKey;

// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab

/**
 * @author Krapht I have no bloody clue what different mods use to differate
 * between items except for itemID, there is metadata, damage, and
 * whatnot. so..... to avoid having to change all my bloody code every
 * time I need to support a new item targeted that would make it a
 * "different" item, I made this cache here A ItemIdentifier is
 * immutable, singleton and most importantly UNIQUE!
 */
public final class ItemIdentifier implements Comparable<ItemIdentifier>, ILPCCTypeHolder {

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemIdentifier> STREAM_CODEC =
        StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            identifier -> Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(identifier.item),
                "item is not registered"),
            DataComponentPatch.STREAM_CODEC,
            identifier -> identifier.components,
            (key, patch) -> ItemIdentifier.get(BuiltInRegistries.ITEM.getValue(key), patch));

    private static final Predicate<DataComponentType<?>> IS_DAMAGE = t -> t == DataComponents.DAMAGE;
    private static final Predicate<DataComponentType<?>> IS_NOT_DAMAGE = t -> t != DataComponents.DAMAGE;
    private static final AtomicLong serialCounter = new AtomicLong();
    // Identifiers whose patch is empty. Bounded by the item registry, so these are held strongly.
    private final static ConcurrentHashMap<Item, ItemIdentifier> simpleIdentifiers = new ConcurrentHashMap<>(4096, 0.5f,
        1);
    // Everything else. Held weakly: with damage folded into the patch this tier absorbs every
    // durability value of every tool and every enchanted book, which is unbounded and player-driven.
    private final static WeakInternCache<ItemKey, ItemIdentifier> patchedIdentifiers =
        WeakInternCache.create("LogisticsPipes ItemIdentifier Cleanup Thread");
    // Item -> registry-key path of the FIRST CATEGORY tab containing it, built once on demand.
    // Vanilla only builds tab contents from the client creative-inventory screen, so on a
    // dedicated server (and on survival clients) getDisplayItems() stays empty forever; we
    // build the contents ourselves instead of depending on that screen having been opened.
    @Nullable
    private static volatile Map<Item, String> creativeTabNameByItem = null;
    public final Item item;
    /**
     * The component patch that distinguishes this identity from the item's prototype. Always
     * canonical (sanitized), see {@link #get(Item, DataComponentPatch)}.
     */
    public final DataComponentPatch components;
    private final Object[] ccTypeHolder = new Object[1];
    /**
     * Allocation-order tiebreaker, so that {@link #compareTo} can be a total order consistent with
     * {@link #equals} even when two identities render the same {@link #sortKey()}. Replaces the old
     * per-Item {@code uniqueID}.
     */
    private final long serial;
    private int maxStackSize = 0;
    @Nullable
    private String sortKey = null;
    @Nullable
    private ItemIdentifier idIgnoringNBT = null;
    @Nullable
    private ItemIdentifier idIgnoringDamage = null;
    @Nullable
    private ItemIdentifier idIgnoringData = null;
    @Nullable
    private DictItemIdentifier dict;
    private boolean canHaveDict = true;
    @Nullable
    private String modName;
    @Nullable
    private String creativeTabName;

    //Hide default constructor
    private ItemIdentifier(Item item, DataComponentPatch components) {
        this.item = item;
        this.components = components;
        this.serial = ItemIdentifier.serialCounter.getAndIncrement();
    }

    private static ItemIdentifier getOrCreateSimple(Item item) {
        //no locking here. if 2 threads race and create the same ItemIdentifier, they end up .equal() and one of them ends up in the map
        ItemIdentifier ret = ItemIdentifier.simpleIdentifiers.get(item);
        if (ret != null) {
            return ret;
        }
        ret = new ItemIdentifier(item, DataComponentPatch.EMPTY);
        ItemIdentifier.simpleIdentifiers.put(item, ret);
        return ret;
    }

    private static ItemIdentifier getOrCreatePatched(ItemKey k) {
        return ItemIdentifier.patchedIdentifiers.getOrCreate(k,
            key -> new ItemIdentifier(key.item(), key.components()));
    }

    /**
     * Interns the identity of {@code item} carrying {@code rawPatch}.
     * <p>
     * A DataComponentPatch is not canonical on its own: a patch that sets a component to the value
     * the item's prototype already has is a distinct patch from {@link DataComponentPatch#EMPTY} but
     * yields an identical ItemStack. Patches arriving from the network, from disk or from the
     * ComputerCraft builder are therefore normalized by round-tripping through an ItemStack, whose
     * {@link ItemStack#getComponentsPatch()} is canonical by construction -- otherwise the same item
     * could end up with two different ItemIdentifiers.
     */
    public static ItemIdentifier get(Item item, DataComponentPatch rawPatch) {
        if (rawPatch.isEmpty()) {
            return ItemIdentifier.getOrCreateSimple(item);
        }
        // Fast path: the patch is already canonical because we have seen it before.
        ItemIdentifier hit = ItemIdentifier.patchedIdentifiers.getIfPresent(new ItemKey(item, rawPatch));
        if (hit != null) {
            return hit;
        }
        return ItemIdentifier.get(new ItemStack(item.builtInRegistryHolder(), 1, rawPatch));
    }

    /**
     * The bare identity of {@code item}, carrying no components at all.
     */
    public static ItemIdentifier get(Item item) {
        return ItemIdentifier.getOrCreateSimple(item);
    }

    public static ItemIdentifier get(ItemStack itemStack) {
        DataComponentPatch patch = itemStack.getComponentsPatch();
        if (patch.isEmpty()) {
            return ItemIdentifier.getOrCreateSimple(itemStack.getItem());
        }
        return ItemIdentifier.getOrCreatePatched(new ItemKey(itemStack.getItem(), patch));
    }

    /**
     * Returns every interned identity of {@code item} whose damage matches, i.e. all component
     * variants of that item/damage pair. ComputerCraft API surface only.
     */
    public static List<ItemIdentifier> getMatchingNBTIdentifier(Item item, int itemData) {
        //inefficient, we'll have to add another map if this becomes a bottleneck
        ArrayList<ItemIdentifier> resultlist = new ArrayList<>(16);
        ItemIdentifier.patchedIdentifiers.forEachValue(t -> {
            if (t.item == item && t.getDamageValue() == itemData) {
                resultlist.add(t);
            }
        });
        return resultlist;
    }

    private static Map<Item, String> getCreativeTabNameMap() {
        Map<Item, String> map = ItemIdentifier.creativeTabNameByItem;
        if (map != null) {
            return map;
        }
        synchronized (ItemIdentifier.class) {
            if (ItemIdentifier.creativeTabNameByItem != null) {
                return ItemIdentifier.creativeTabNameByItem;
            }
            map = new HashMap<>();
            CreativeModeTab.ItemDisplayParameters params =
                new CreativeModeTab.ItemDisplayParameters(
                    FeatureFlags.REGISTRY.allFlags(), false,
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                // SEARCH aggregates every other tab's items and HOTBAR/INVENTORY are synthetic;
                // only CATEGORY tabs correspond to LP1's per-item CreativeTabs#tabLabel.
                if (tab == null || tab.getType() != CreativeModeTab.Type.CATEGORY) {
                    continue;
                }
                ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab)
                    .map(ResourceKey::location).orElse(null);
                String tabName = key != null ? key.getPath() : tab.getDisplayName().getString();
                // The client's creative screen may rebuild tab contents concurrently (integrated
                // server thread vs render thread), so guard the whole per-tab read.
                try {
                    Collection<ItemStack> displayItems = tab.getDisplayItems();
                    if (displayItems.isEmpty()) {
                        tab.buildContents(params);
                        displayItems = tab.getDisplayItems();
                    }
                    for (ItemStack stack : displayItems) {
                        if (stack != null && !stack.isEmpty()) {
                            map.putIfAbsent(stack.getItem(), tabName);
                        }
                    }
                } catch (Exception e) {
                    LogisticsPipes.LOG.warn("Failed to read creative tab contents for {}", tabName, e);
                }
            }
            ItemIdentifier.creativeTabNameByItem = map;
            return map;
        }
    }

    private static Map<Integer, Object> getArrayAsMap(int[] array) {
        HashMap<Integer, Object> map = new HashMap<>();
        int i = 0;
        for (int object : array) {
            map.put(i, object);
            i++;
        }
        return map;
    }

    private static Map<Integer, Object> getArrayAsMap(byte[] array) {
        HashMap<Integer, Object> map = new HashMap<>();
        int i = 1;
        for (byte object : array) {
            map.put(i, object);
            i++;
        }
        return map;
    }

    @Nullable
    public static Map<Object, Object> getNBTBaseAsMap(@Nullable Tag nbt)
        throws SecurityException, IllegalArgumentException {
        if (nbt == null) {
            return null;
        }

        if (nbt instanceof ByteTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "ByteTag");
            map.put("value", ((ByteTag) nbt).getAsByte());
            return map;
        } else if (nbt instanceof ByteArrayTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "ByteArrayTag");
            map.put("value", ItemIdentifier.getArrayAsMap(((ByteArrayTag) nbt).getAsByteArray()));
            return map;
        } else if (nbt instanceof DoubleTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "DoubleTag");
            map.put("value", ((DoubleTag) nbt).getAsDouble());
            return map;
        } else if (nbt instanceof FloatTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "FloatTag");
            map.put("value", ((FloatTag) nbt).getAsFloat());
            return map;
        } else if (nbt instanceof IntTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "IntTag");
            map.put("value", ((IntTag) nbt).getAsInt());
            return map;
        } else if (nbt instanceof IntArrayTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "IntArrayTag");
            map.put("value", ItemIdentifier.getArrayAsMap(((IntArrayTag) nbt).getAsIntArray()));
            return map;
        } else if (nbt instanceof ListTag) {
            HashMap<Integer, Object> content = new HashMap<>();
            int i = 1;
            for (Object object : ((ListTag) nbt)) {
                if (object instanceof Tag) {
                    content.put(i, ItemIdentifier.getNBTBaseAsMap((Tag) object));
                }
                i++;
            }
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "ListTag");
            map.put("value", content);
            return map;
        } else if (nbt instanceof CompoundTag) {
            HashMap<Object, Object> content = new HashMap<>();
            HashMap<Integer, Object> keys = new HashMap<>();
            int i = 1;
            for (String key : ((CompoundTag) nbt).getAllKeys()) {
                Tag value = ((CompoundTag) nbt).get(key);
                content.put(key, ItemIdentifier.getNBTBaseAsMap(value));
                keys.put(i, key);
                i++;
            }
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "CompoundTag");
            map.put("value", content);
            map.put("keys", keys);
            return map;
        } else if (nbt instanceof LongTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "LongTag");
            map.put("value", ((LongTag) nbt).getAsLong());
            return map;
        } else if (nbt instanceof ShortTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "ShortTag");
            map.put("value", ((ShortTag) nbt).getAsShort());
            return map;
        } else if (nbt instanceof StringTag) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("type", "StringTag");
            map.put("value", nbt.getAsString());
            return map;
        } else {
            throw new UnsupportedOperationException(
                "Unsupported net.minecraft.nbt.Tag of type:" + nbt.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static String renderComponent(DataComponentType<?> type, Object value) {
        Codec<Object> codec = (Codec<Object>) type.codec();
        if (codec != null) {
            // Plain NbtOps: values referencing a datapack registry cannot be encoded without
            // RegistryOps and fall through to toString below, which is content-based for the
            // records component values are made of. Either way `serial` keeps the order total.
            Optional<String> encoded = codec.encodeStart(NbtOps.INSTANCE, value)
                .result().map(Tag::toString);
            if (encoded.isPresent()) {
                return encoded.get();
            }
        }
        return String.valueOf(value);
    }

    /* Instance Methods */

    /**
     * The damage this identity carries, 0 when it carries none. In 1.21 damage is just another data
     * component; this accessor exists so consumers do not have to reach into the patch themselves.
     */
    public int getDamageValue() {
        Optional<? extends Integer> damage = components.get(DataComponents.DAMAGE);
        return damage != null && damage.isPresent() ? damage.get() : 0;
    }

    public boolean hasCustomData() {
        Optional<? extends CustomData> data = components.get(DataComponents.CUSTOM_DATA);
        return data != null && data.isPresent();
    }

    /**
     * A mutable copy of this identity's {@link DataComponents#CUSTOM_DATA}, or null when it has
     * none. This is the closest equivalent of the old {@code tag} field.
     */
    @Nullable
    public CompoundTag getCustomDataTag() {
        Optional<? extends CustomData> data = components.get(DataComponents.CUSTOM_DATA);
        return data != null && data.isPresent() ? data.get().copyTag() : null;
    }

    private ItemIdentifier project(DataComponentPatch projected) {
        return projected.equals(components) ? this : ItemIdentifier.get(item, projected);
    }

    /**
     * This identity with its damage dropped, if the item is damageable at all.
     */
    public ItemIdentifier getUndamaged() {
        if (idIgnoringDamage == null) {
            idIgnoringDamage = isDamageable() ? project(components.forget(ItemIdentifier.IS_DAMAGE)) : this;
        }
        return idIgnoringDamage;
    }

    /**
     * This identity with everything <i>except</i> damage dropped.
     * <p>
     * In 1.12 an ItemStack was Item + damage + NBT tag, where damage was a field separate from the
     * tag; "ignoring NBT" therefore kept the damage. In 1.21 damage is itself a data component, so
     * that behaviour has to be spelled out as "keep only DAMAGE" -- otherwise this projection would
     * collapse into {@link #getUndamaged()} and the IGNORE_NBT / IGNORE_DAMAGE fuzzy flags would
     * stop being independent.
     */
    public ItemIdentifier getIgnoringNBT() {
        if (idIgnoringNBT == null) {
            idIgnoringNBT = project(components.forget(ItemIdentifier.IS_NOT_DAMAGE));
        }
        return idIgnoringNBT;
    }

    /**
     * This identity with its damage dropped, damageable or not. Differs from {@link #getUndamaged()}
     * only in skipping the damageable check.
     */
    public ItemIdentifier getIgnoringData() {
        if (idIgnoringData == null) {
            idIgnoringData = project(components.forget(ItemIdentifier.IS_DAMAGE));
        }
        return idIgnoringData;
    }

    private String getName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    public String getFriendlyName() {
        return getName(makeNormalStack(1));
    }

    public String getFriendlyNameCC() {
        return MainProxy.proxy.getName(this);
    }

    public String getModName() {
        if (modName == null) {
            ResourceLocation rl = BuiltInRegistries.ITEM.getResourceKey(item)
                .map(ResourceKey::location).orElse(null);
            if (rl != null) {
                modName = ModList.get().getModContainerById(rl.getNamespace())
                    .map(mc -> mc.getModInfo().getDisplayName())
                    .orElse("UNKNOWN");
            } else {
                modName = "UNKNOWN";
            }
        }
        return modName;
    }

    public String getCreativeTabName() {
        // In 1.20.1 items are no longer bound to a single creative tab (Item#getCreativeTab
        // was removed). We resolve the FIRST CATEGORY tab containing this item, mirroring
        // LP1's CreativeTabs#tabLabel behaviour. The same string is both stored (via the GUI's
        // getStringForItem -> getCreativeTabName) and compared (ModuleCreativeTabBasedItemSink
        // #sinksItem -> tabList.contains(getCreativeTabName())), so any stable form keeps them
        // matched. We use the tab's registry key path which is stable across sessions.
        if (creativeTabName == null) {
            creativeTabName = ItemIdentifier.getCreativeTabNameMap().get(item);
        }
        return creativeTabName;
    }

    public ItemIdentifierStack makeStack(int stackSize) {
        return new ItemIdentifierStack(this, stackSize);
    }

    /**
     * A real ItemStack carrying this identity. There used to be an "unsafe" variant of this that
     * shared the tag instead of copying it; the distinction is obsolete in the component model, so
     * the two were collapsed. {@code PatchedDataComponentMap.fromPatch} shares the patch
     * copy-on-write, so any write to the returned stack copies first and the interned patch cannot
     * be mutated through it, and component values are immutable by contract.
     */
    public ItemStack makeNormalStack(int stackSize) {
        return new ItemStack(item.builtInRegistryHolder(), stackSize, components);
    }

    public ItemEntity makeEntityItem(int stackSize, Level level, double x, double y, double z) {
        return new ItemEntity(level, x, y, z, makeNormalStack(stackSize));
    }

    public int getMaxStackSize() {
        if (maxStackSize == 0) {
            ItemStack tstack = makeNormalStack(1);
            int tstacksize = tstack.getMaxStackSize();
            if (tstack.isDamageableItem() && tstack.isDamaged()) {
                tstacksize = 1;
            }
            tstacksize = Math.clamp(tstacksize, 1, 64);
            maxStackSize = tstacksize;
        }
        return maxStackSize;
    }

    @Override
    public String toString() {
        return getModName() + ":" + getFriendlyName() + ", " + BuiltInRegistries.ITEM.getId(item) + ":"
            + getDamageValue();
    }

    /**
     * A deterministic rendering of {@link #components}, used to give the component half of the
     * identity a stable sort position.
     * <p>
     * Entries are sorted by the component type's registry key, so the result is stable across runs
     * and between client and server. {@code DataComponentPatch.toString()} cannot be used for this:
     * it iterates a {@code Reference2ObjectArrayMap} in insertion order, which is not canonical.
     */
    private String sortKey() {
        String key = sortKey;
        if (key != null) {
            return key;
        }
        if (components.isEmpty()) {
            return sortKey = "";
        }
        List<String> parts = new ArrayList<>(components.size());
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : components.entrySet()) {
            ResourceLocation typeKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            String name = typeKey != null ? typeKey.toString() : entry.getKey().toString();
            // An empty Optional means "removed relative to the prototype", not "absent".
            parts.add(entry.getValue()
                .map(value -> name + "=" + ItemIdentifier.renderComponent(entry.getKey(), value))
                .orElse("!" + name));
        }
        parts.sort(null);
        return sortKey = String.join(",", parts);
    }

    /**
     * A total order consistent with {@link #equals}. That consistency is load-bearing:
     * {@code ServerRouter} keeps its routing interests in a {@code TreeSet<ItemIdentifier>}, so an
     * order that reported unequal identities as equal would silently drop interests.
     */
    @Override
    public int compareTo(ItemIdentifier o) {
        if (this == o) {
            return 0;
        }
        int c = Integer.compare(BuiltInRegistries.ITEM.getId(item), BuiltInRegistries.ITEM.getId(o.item));
        if (c != 0) {
            return c;
        }
        c = Integer.compare(getDamageValue(), o.getDamageValue());
        if (c != 0) {
            return c;
        }
        c = sortKey().compareTo(o.sortKey());
        if (c != 0) {
            return c;
        }
        // Only reached when two distinct identities render identically, i.e. they differ solely in
        // components that could not be encoded. Keeps the order total and consistent with equals.
        return Long.compare(serial, o.serial);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof ItemIdentifierStack) {
            throw new IllegalStateException(
                "Comparison between ItemIdentifierStack and ItemIdentifier -- did you forget a .getItem() in your code?");
        }
        if (!(that instanceof ItemIdentifier i)) {
            return false;
        }
        return this.equals(i);

    }

    public boolean equals(@Nullable ItemIdentifier that) {
        if (that == null) {
            return false;
        }
        return item == that.item && components.equals(that.components);
    }

    @Override
    public int hashCode() {
        return item.hashCode() * 31 + components.hashCode();
    }

    public boolean equalsForCrafting(ItemIdentifier item) {
        return this.item == item.item && (item.isDamageable() || (getDamageValue() == item.getDamageValue()));
    }

    public boolean equalsWithoutNBT(ItemStack stack) {
        return item == stack.getItem() && getDamageValue() == stack.getDamageValue();
    }

    /**
     * Item and damage match, everything else ignored. Equivalent to comparing the two
     * {@link #getIgnoringNBT()} projections.
     */
    public boolean equalsWithoutNBT(ItemIdentifier item) {
        return this.item == item.item && getDamageValue() == item.getDamageValue();
    }

    public boolean isDamageable() {
        return makeNormalStack(1).isDamageableItem();
    }

    public boolean isFluidContainer() {
        return item instanceof LogisticsFluidContainer;
    }

    @Nullable
    public DictItemIdentifier getDictIdentifiers() {
        if (dict == null && canHaveDict) {
            dict = DictItemIdentifier.getDictItemIdentifier(this);
            canHaveDict = false;
        }
        return dict;
    }

    public void debugDumpData(boolean isClient) {
        StringBuilder sb = new StringBuilder();
        sb.append(isClient ? "Client" : "Server").append(" Item: ")
            .append(BuiltInRegistries.ITEM.getId(item)).append(':').append(getDamageValue())
            .append(" serial ").append(serial).append('\n');
        sb.append("Components: ").append(sortKey()).append('\n');
        sb.append("CustomData: ");
        debugDumpTag(getCustomDataTag(), sb);
        sb.append('\n');
        sb.append("Damageable: ").append(isDamageable()).append('\n');
        sb.append("MaxStackSize: ").append(getMaxStackSize()).append('\n');
        if (getUndamaged() == this) {
            sb.append("Undamaged: this\n");
        } else {
            sb.append("Undamaged: (see recursive dump)\n");
            getUndamaged().debugDumpData(isClient);
        }
        sb.append("Mod: ").append(getModName()).append('\n');
        sb.append("CreativeTab: ").append(getCreativeTabName());
        LogisticsPipes.LOG.info("{}", sb);
        if (getDictIdentifiers() != null) {
            getDictIdentifiers().debugDumpData(isClient);
        }
    }

    private void debugDumpTag(Tag nbt, StringBuilder sb) {
        if (nbt == null) {
            sb.append("null");
            return;
        }
        if (nbt instanceof ByteTag) {
            sb.append("TagByte(data=").append(((ByteTag) nbt).getAsByte()).append(")");
        } else if (nbt instanceof ShortTag) {
            sb.append("TagShort(data=").append(((ShortTag) nbt).getAsShort()).append(")");
        } else if (nbt instanceof IntTag) {
            sb.append("TagInt(data=").append(((IntTag) nbt).getAsInt()).append(")");
        } else if (nbt instanceof LongTag) {
            sb.append("TagLong(data=").append(((LongTag) nbt).getAsLong()).append(")");
        } else if (nbt instanceof FloatTag) {
            sb.append("TagFloat(data=").append(((FloatTag) nbt).getAsFloat()).append(")");
        } else if (nbt instanceof DoubleTag) {
            sb.append("TagDouble(data=").append(((DoubleTag) nbt).getAsDouble()).append(")");
        } else if (nbt instanceof StringTag) {
            sb.append("TagString(data=\"").append(nbt.getAsString()).append("\")");
        } else if (nbt instanceof ByteArrayTag) {
            sb.append("TagByteArray(data=");
            for (int i = 0; i < ((ByteArrayTag) nbt).getAsByteArray().length; i++) {
                sb.append(((ByteArrayTag) nbt).getAsByteArray()[i]);
                if (i < ((ByteArrayTag) nbt).getAsByteArray().length - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");
        } else if (nbt instanceof IntArrayTag) {
            sb.append("TagIntArray(data=");
            for (int i = 0; i < ((IntArrayTag) nbt).getAsIntArray().length; i++) {
                sb.append(((IntArrayTag) nbt).getAsIntArray()[i]);
                if (i < ((IntArrayTag) nbt).getAsIntArray().length - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");
        } else if (nbt instanceof ListTag) {
            sb.append("TagList(data=");
            for (int i = 0; i < ((ListTag) nbt).size(); i++) {
                debugDumpTag((((ListTag) nbt).get(i)), sb);
                if (i < ((ListTag) nbt).size() - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");
        } else if (nbt instanceof CompoundTag) {
            sb.append("TagCompound(data=");
            for (Iterator<String> iter = ((CompoundTag) nbt).getAllKeys().iterator(); iter.hasNext(); ) {
                String key = iter.next();
                Tag value = ((CompoundTag) nbt).get(key);
                sb.append("\"").append(key).append("\"=");
                debugDumpTag((value), sb);
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append(")");
        } else {
            sb.append(nbt.getClass().getName()).append("(?)");
        }
    }

    @Override
    public Object[] getTypeHolder() {
        return ccTypeHolder;
    }

    // A key to look up an ItemIdentifier by Item + component patch. DataComponentPatch is immutable
    // and has value-based equals/hashCode, so a record is all the bookkeeping this needs -- the
    // per-Item BitSet of tag ids that used to give the NBT half of the identity a comparable int is
    // gone, DataComponentPatch.equals does that job directly.
    private record ItemKey(Item item, DataComponentPatch components) {}
}
