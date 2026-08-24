package logisticspipes.integrations.refinedstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import org.jspecify.annotations.Nullable;

import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.inventory.ProviderMode;

/**
 * Exposes the contents of a Refined Storage network to Logistics Pipes, the counterpart of
 * {@link logisticspipes.integrations.ae2.AEInterfaceInventoryHandler}.
 *
 * <p>Reads, extracts and inserts. Insertion is not optional here either: {@code InventoryUtilFactory}
 * and {@code InventoryHelper} consult the special handlers <i>before</i> the block's
 * {@code IItemHandler} capability, so claiming a block also takes over its delivery path.</p>
 *
 * <p>The way in is the capability RS publishes for exactly this purpose,
 * {@code RefinedStorageNeoForgeApi#getNetworkNodeContainerProviderCapability}, rather than an
 * instanceof against RS's block entity classes. One caveat: {@link ItemResource} lives in RS's
 * {@code common.support.resource} package rather than under {@code common.api}, so that one type is
 * an implementation detail we depend on and the most likely thing to break on an RS update. It is
 * the only non-API class used here.</p>
 */
public class RSInterfaceInventoryHandler extends SpecialInventoryHandler implements SpecialInventoryHandler.Factory {

    /**
     * Null on the instance registered as a factory; set on the instances it hands out.
     */
    @Nullable
    private final StorageNetworkComponent storage;
    /**
     * How many of each type to leave behind. An RS network is slotless, so as with AE2 the per-slot
     * provider modes have no counterpart: "leave first/last slot" is dropped, and both "leave one
     * per stack" and "leave one per type" collapse to leaving one of each type.
     */
    private final int reservePerType;

    public RSInterfaceInventoryHandler() {
        this(null, 0);
    }

    private RSInterfaceInventoryHandler(@Nullable StorageNetworkComponent storage, int reservePerType) {
        this.storage = storage;
        this.reservePerType = reservePerType;
    }

    /* Factory */

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean isType(BlockEntity tile, @Nullable Direction dir) {
        return RSNetworks.findStorage(tile, dir) != null;
    }

    @Nullable
    @Override
    public SpecialInventoryHandler getUtilForTile(BlockEntity tile, @Nullable Direction direction, ProviderMode mode) {
        StorageNetworkComponent found = RSNetworks.findStorage(tile, direction);
        if (found == null) {
            return null;
        }
        int reserve = mode.getHideOnePerStack() || mode.getHideOnePerType() ? 1 : 0;
        return new RSInterfaceInventoryHandler(found, reserve);
    }

    /* Queries */

    private StorageNetworkComponent storage() {
        if (storage == null) {
            throw new IllegalStateException("RS handler used as a factory; call getUtilForTile first");
        }
        return storage;
    }

    /**
     * How much may be taken of a resource holding {@code stored}, after the reserve.
     */
    private int available(long stored) {
        long usable = stored - reservePerType;
        if (usable <= 0) {
            return 0;
        }
        // An RS network can hold far more than an int; every LP count is an int.
        return (int) Math.min(usable, Integer.MAX_VALUE);
    }

    @Override
    public Map<ItemIdentifier, Integer> getItemsAndCount() {
        Map<ItemIdentifier, Integer> result = new HashMap<>();
        for (ResourceAmount entry : storage().getAll()) {
            if (!(entry.resource() instanceof ItemResource item)) {
                continue;
            }
            int count = available(entry.amount());
            if (count <= 0) {
                continue;
            }
            result.merge(ItemIdentifier.get(item.toItemStack()), count, Integer::sum);
        }
        return result;
    }

    @Override
    public Set<ItemIdentifier> getItems() {
        return new HashSet<>(getItemsAndCount().keySet());
    }

    @Override
    public boolean containsUndamagedItem(ItemIdentifier item) {
        for (ResourceAmount entry : storage().getAll()) {
            if (!(entry.resource() instanceof ItemResource resource) || available(entry.amount()) <= 0) {
                continue;
            }
            if (ItemIdentifier.get(resource.toItemStack()).getUndamaged().equals(item)) {
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
     * Overrides the default, which would call {@link #getSingleItem} {@code count} times and so make
     * {@code count} separate round trips through the network.
     */
    @Override
    public ItemStack getMultipleItems(ItemIdentifier item, int count) {
        return extract(item, count);
    }

    private ItemStack extract(ItemIdentifier item, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        ResourceKey key = ItemResource.ofItemStack(item.makeNormalStack(1));
        StorageNetworkComponent inventory = storage();
        int limit = available(inventory.get(key));
        if (limit <= 0) {
            return ItemStack.EMPTY;
        }
        long extracted = inventory.extract(key, Math.min(count, limit), Action.EXECUTE, RSNetworks.ACTOR);
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        return item.makeNormalStack((int) extracted);
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
     * neighbour with {@code util.getContainerSize() > 0}, so reporting the bare number of stored
     * types would make a pipe refuse to connect to an empty network — and disconnect from one that
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
        long accepted = storage().insert(ItemResource.ofItemStack(stack), stack.getCount(), Action.SIMULATE,
            RSNetworks.ACTOR);
        return (int) Math.min(accepted, Integer.MAX_VALUE);
    }

    /**
     * Follows {@link logisticspipes.utils.transactor.Transactor#add}: the returned stack carries the
     * amount that was <i>accepted</i>, not the leftover. Getting this backwards destroys items --
     * {@code PipeTransportLogistics#insertArrivingItem} subtracts the returned count from the
     * travelling stack and drops the rest.
     */
    @Override
    public ItemStack add(ItemStack stack, Direction from, boolean doAdd) {
        ItemStack added = stack.copy();
        added.setCount(0);
        if (stack.isEmpty()) {
            return added;
        }
        long inserted = storage().insert(ItemResource.ofItemStack(stack), stack.getCount(),
            doAdd ? Action.EXECUTE : Action.SIMULATE, RSNetworks.ACTOR);
        added.setCount((int) Math.min(inserted, Integer.MAX_VALUE));
        return added;
    }
}
