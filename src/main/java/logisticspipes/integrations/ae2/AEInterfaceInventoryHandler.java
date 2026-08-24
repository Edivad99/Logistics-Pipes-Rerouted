package logisticspipes.integrations.ae2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jspecify.annotations.Nullable;

import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.inventory.ProviderMode;

/**
 * Exposes the contents of an Applied Energistics 2 network to Logistics Pipes, so that a provider
 * pipe placed against an ME Interface (or any other block that hosts a grid node) can see and
 * supply what the network stores.
 *
 * <p>Reads, extracts and inserts. Insertion is not optional: {@code InventoryUtilFactory} and
 * {@code InventoryHelper} both consult the special handlers <i>before</i> falling back to the
 * block's {@code IItemHandler} capability, so as soon as this handler claims a block it also owns
 * the delivery path. A handler that refused insertion would not merely lack a feature, it would
 * take away the capability-based insertion that worked before it existed.</p>
 *
 * <p>An ME network has no slots, so the {@code AbstractContainerMenu} adapter part of
 * {@link logisticspipes.interfaces.IInventoryUtil} is served by a synthetic slot view: one slot per
 * distinct item type, ordered by {@link ItemIdentifier#compareTo}. That order is deterministic, but
 * the view is only as stable as the network itself — an index is not a durable handle.</p>
 */
public class AEInterfaceInventoryHandler extends SpecialInventoryHandler implements SpecialInventoryHandler.Factory {

    /**
     * Null on the instance registered as a factory; set on the instances it hands out.
     */
    @Nullable
    private final MEStorage storage;
    private final IActionSource source;
    /**
     * How many of each type to leave behind. An ME network is slotless, so the per-slot provider
     * modes have no counterpart here: "leave first/last slot" is dropped, and both "leave one per
     * stack" and "leave one per type" collapse to leaving one of each type.
     */
    private final int reservePerType;

    public AEInterfaceInventoryHandler() {
        this(null, IActionSource.empty(), 0);
    }

    private AEInterfaceInventoryHandler(@Nullable MEStorage storage, IActionSource source, int reservePerType) {
        this.storage = storage;
        this.source = source;
        this.reservePerType = reservePerType;
    }

    /* Factory */

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean isType(BlockEntity tile, @Nullable Direction dir) {
        return AE2Networks.findStorage(tile, dir) != null;
    }

    @Nullable
    @Override
    public SpecialInventoryHandler getUtilForTile(BlockEntity blockEntity, @Nullable Direction direction,
        ProviderMode mode) {
        MEStorage found = AE2Networks.findStorage(blockEntity, direction);
        if (found == null) {
            return null;
        }
        int reserve = mode.getHideOnePerStack() || mode.getHideOnePerType() ? 1 : 0;
        return new AEInterfaceInventoryHandler(found, AE2Networks.actionSource(blockEntity), reserve);
    }

    /* Queries */

    private MEStorage storage() {
        if (storage == null) {
            throw new IllegalStateException("AE2 handler used as a factory; call getUtilForTile first");
        }
        return storage;
    }

    /**
     * How much of {@code key} may be taken, after honouring {@link #reservePerType}.
     */
    private int available(long stored) {
        long usable = stored - reservePerType;
        if (usable <= 0) {
            return 0;
        }
        // An ME network can hold far more than an int; every LP count is an int.
        return (int) Math.min(usable, Integer.MAX_VALUE);
    }

    @Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() {
        Map<ItemIdentifier, Integer> result = new HashMap<>();
        for (Object2LongMap.Entry<AEKey> entry : storage().getAvailableStacks()) {
            if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                continue;
            }
            int count = available(entry.getLongValue());
            if (count <= 0) {
                continue;
            }
            // getReadOnlyStack() avoids a copy; ItemIdentifier.get only reads from it.
            result.merge(ItemIdentifier.get(itemKey.getReadOnlyStack()), count, Integer::sum);
        }
        return result;
    }

    @Override
    public Set<ItemIdentifier> getItems() {
        return new HashSet<>(getItemsAndCount().keySet());
    }

    @Override
    public boolean containsUndamagedItem(ItemIdentifier item) {
        for (Object2LongMap.Entry<AEKey> entry : storage().getAvailableStacks()) {
            if (!(entry.getKey() instanceof AEItemKey itemKey) || available(entry.getLongValue()) <= 0) {
                continue;
            }
            if (ItemIdentifier.get(itemKey.getReadOnlyStack()).getUndamaged().equals(item)) {
                return true;
            }
        }
        return false;
    }

    /* Extraction */

    @Override
    public ItemStack getSingleItem(ItemIdentifier item) {
        return extract(item, 1);
    }

    /**
     * Overrides the default, which would call {@link #getSingleItem} {@code count} times and so
     * make {@code count} separate round trips through the network.
     */
    @Override
    public ItemStack getMultipleItems(ItemIdentifier item, int count) {
        return extract(item, count);
    }

    private ItemStack extract(ItemIdentifier item, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = AEItemKey.of(item.makeNormalStack(1));
        if (key == null) {
            return ItemStack.EMPTY;
        }
        MEStorage inventory = storage();
        int limit = available(inventory.getAvailableStacks().get(key));
        if (limit <= 0) {
            return ItemStack.EMPTY;
        }
        long extracted = inventory.extract(key, Math.min(count, limit), Actionable.MODULATE, source);
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        return key.toStack((int) extracted);
    }

    /* Slot adapter over a slotless network */

    private List<Map.Entry<ItemIdentifier, Integer>> snapshot() {
        List<Map.Entry<ItemIdentifier, Integer>> entries = new ArrayList<>(getItemsAndCount().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        return entries;
    }

    /**
     * The types currently stored, plus one for the room a slotless network always has.
     *
     * <p>The {@code + 1} is load-bearing and must not be "simplified" away: {@code
     * PipeTransportLogistics#canPipeConnect_internal} decides whether a pipe may connect to a
     * neighbor with {@code util.getContainerSize() > 0}, so reporting the bare number of stored
     * types would make a pipe refuse to connect to an empty network -- and disconnect from one that
     * became empty.</p>
     */
    @Override
    public int getContainerSize() {
        return getItemsAndCount().size() + 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        List<Map.Entry<ItemIdentifier, Integer>> entries = snapshot();
        if (slot < 0 || slot >= entries.size()) {
            return ItemStack.EMPTY;
        }
        Map.Entry<ItemIdentifier, Integer> entry = entries.get(slot);
        return entry.getKey().makeNormalStack(entry.getValue());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        List<Map.Entry<ItemIdentifier, Integer>> entries = snapshot();
        if (slot < 0 || slot >= entries.size()) {
            return ItemStack.EMPTY;
        }
        return extract(entries.get(slot).getKey(), amount);
    }

    /* Insertion */

    @Override
    public int roomForItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        long accepted = storage().insert(key, stack.getCount(), Actionable.SIMULATE, source);
        return (int) Math.min(accepted, Integer.MAX_VALUE);
    }

    /**
     * Follows {@link logisticspipes.utils.transactor.Transactor#add}: the returned stack carries the
     * amount that was <i>accepted</i>, not the leftover. Getting this backwards destroys items --
     * {@code PipeTransportLogistics#insertArrivingItem} subtracts the returned count from the
     * traveling stack and drops the rest.
     */
    @Override
    public ItemStack add(ItemStack stack, Direction from, boolean doAdd) {
        ItemStack added = stack.copy();
        added.setCount(0);
        if (stack.isEmpty()) {
            return added;
        }
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return added;
        }
        long inserted = storage().insert(key, stack.getCount(),
            doAdd ? Actionable.MODULATE : Actionable.SIMULATE, source);
        added.setCount((int) Math.min(inserted, Integer.MAX_VALUE));
        return added;
    }
}
