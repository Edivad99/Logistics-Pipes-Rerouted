package logisticspipes.transport;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Iterables;
import org.jspecify.annotations.Nullable;

/**
 * The items traveling through one pipe, by id, in the order they came in.
 *
 * <p>The pipe walks the list every tick in {@link #forEachDeferred}, and that is exactly when items
 * leave it, come into it or get sent somewhere else. So the list is never changed under the walk:
 * changes made meanwhile wait in a queue and are applied once it is over.</p>
 * <ul>
 * <li>Items that reach the end are {@linkplain #scheduleRemoval scheduled for removal}, and stay if
 * they are {@linkplain #unscheduleRemoval unscheduled} because they found a way after all.</li>
 * <li>Items that come in during the walk are added after it.</li>
 * <li>Items read from disk only join a few ticks after loading, once the pipes around are ready.</li>
 * </ul>
 */
public class LPItemList implements Iterable<LPTravelingItem> {

    /**
     * Ticks the items read from disk wait before joining.
     */
    private static final int LOAD_DELAY = 10;

    private final Map<Integer, LPTravelingItem> items = new LinkedHashMap<>();
    private final Set<LPTravelingItem> toLoad = new LinkedHashSet<>();
    private final Set<LPTravelingItem> toAdd = new LinkedHashSet<>();
    private final Set<LPTravelingItem> toRemove = new LinkedHashSet<>();
    private final PipeTransportLogistics pipe;
    private int loadDelay = 0;
    private boolean iterating = false;

    LPItemList(PipeTransportLogistics pipe) {
        this.pipe = pipe;
    }

    /**
     * Brings the list up to date, runs {@code action} on every item, then applies what changed
     * during the walk: the removals first, then the items that came in.
     */
    void forEachDeferred(Consumer<LPTravelingItem> action) {
        loadScheduledItems();
        removeScheduledItems();
        purgeBadItems();
        iterating = true;
        try {
            items.values().forEach(action);
        } finally {
            iterating = false;
        }
        removeScheduledItems();
        toAdd.forEach(this::add);
        toAdd.clear();
    }

    void add(LPTravelingItem item) {
        if (iterating) {
            toAdd.add(item);
            return;
        }
        if (items.get(item.getId()) == item) {
            return;
        }
        item.setContainer(pipe.container);
        items.put(item.getId(), item);
    }

    @Nullable LPTravelingItem get(int id) {
        return items.get(id);
    }

    /**
     * Takes the item out at once, or after the walk if one is going on.
     */
    void remove(LPTravelingItem item) {
        if (iterating) {
            toRemove.add(item);
        } else {
            items.remove(item.getId(), item);
        }
    }

    void scheduleLoad(LPTravelingItem item) {
        loadDelay = LOAD_DELAY;
        toLoad.add(item);
    }

    /**
     * @return whether the item was not already scheduled
     */
    boolean scheduleRemoval(LPTravelingItem item) {
        return toRemove.add(item);
    }

    void unscheduleRemoval(LPTravelingItem item) {
        toRemove.remove(item);
    }

    /**
     * Every item the pipe holds, including the ones read from disk that have not joined yet: a pipe
     * that has not ticked since it loaded still owns them.
     */
    Iterable<LPTravelingItem> itemsToSave() {
        return Iterables.concat(items.values(), toLoad);
    }

    @Override
    public Iterator<LPTravelingItem> iterator() {
        return items.values().iterator();
    }

    private void loadScheduledItems() {
        if (loadDelay > 0) {
            loadDelay--;
            return;
        }
        toLoad.forEach(this::add);
        toLoad.clear();
    }

    private void removeScheduledItems() {
        items.values().removeAll(toRemove);
        toRemove.clear();
    }

    /**
     * Drops the items left with nothing in them and the ones that have moved on to another pipe.
     */
    private void purgeBadItems() {
        items.values().removeIf(item -> item.isCorrupted() || item.getContainer() != pipe.container);
    }
}
