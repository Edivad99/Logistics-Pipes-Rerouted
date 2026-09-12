package logisticspipes.api.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class ListProperty<T> implements IListProperty<T> {

    private static final String SIZE_NAME = "listSize";
    private static final String ITEM_NAME = "listItem";

    protected final List<T> list;

    private final CopyOnWriteArraySet<ObserverCallback<List<T>>> propertyObservers = new CopyOnWriteArraySet<>();

    protected ListProperty(List<T> list) {
        this.list = list;
    }

    public static String sizeTagKey(String tagKey) {
        return tagKey.isEmpty() ? SIZE_NAME : tagKey + "." + SIZE_NAME;
    }

    public static String itemTagKey(String tagKey, int idx) {
        return tagKey.isEmpty() ? ITEM_NAME + "." + idx : tagKey + "." + ITEM_NAME + "." + idx;
    }

    @Override
    public CopyOnWriteArraySet<ObserverCallback<List<T>>> getPropertyObservers() {
        return propertyObservers;
    }

    public abstract T defaultValue(int idx);

    @Override
    public void ensureSize(int size, IntFunction<T> fillWith) {
        int missing = size - list.size();
        if (missing <= 0) {
            return;
        }
        for (int i = 0; i < missing; i++) {
            list.add(fillWith.apply(list.size()));
        }
        iChanged();
    }

    @Override
    public void ensureSize(int size) {
        ensureSize(size, this::defaultValue);
    }

    @Override
    public boolean replaceContent(Collection<? extends T> col) {
        if (list.equals(new ArrayList<>(col))) {
            return false;
        }
        list.clear();
        list.addAll(col);
        iChanged();
        return true;
    }

    @Override
    public boolean replaceContent(T[] arr) {
        return replaceContent(Arrays.asList(arr));
    }

    // The entries stay flat, one key per index, rather than moving to a ValueOutput list: the
    // element readers are per-key and a ValueInputList is iterable but not indexable.
    @Override
    public void deserialize(ValueInput input) {
        input.getInt(sizeTagKey(getTagKey())).ifPresent(size -> {
            List<T> read = new ArrayList<>(size);
            for (int idx = 0; idx < size; idx++) {
                read.add(readSingleFromNBT(input, itemTagKey(getTagKey(), idx)));
            }
            replaceContent(read);
        });
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt(sizeTagKey(getTagKey()), list.size());
        for (int idx = 0; idx < list.size(); idx++) {
            writeSingleToNBT(output, itemTagKey(getTagKey(), idx), list.get(idx));
        }
    }

    @Override
    public List<T> copyValue() {
        List<T> copy = new ArrayList<>(list.size());
        for (T element : list) {
            copy.add(copyValue(element));
        }
        return copy;
    }

    // ── mutators: notify ─────────────────────────────────────────────────────

    @Override
    public boolean add(T element) {
        boolean added = list.add(element);
        iChanged();
        return added;
    }

    @Override
    public void add(int index, T element) {
        list.add(index, element);
        iChanged();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> elements) {
        boolean added = list.addAll(index, elements);
        iChanged();
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends T> elements) {
        boolean added = list.addAll(elements);
        iChanged();
        return added;
    }

    @Override
    public void clear() {
        list.clear();
        iChanged();
    }

    @Override
    public boolean remove(@Nullable Object element) {
        boolean removed = list.remove(element);
        iChanged();
        return removed;
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        boolean removed = list.removeAll(elements);
        iChanged();
        return removed;
    }

    @Override
    public T remove(int index) {
        T removed = list.remove(index);
        iChanged();
        return removed;
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        list.replaceAll(operator);
        iChanged();
    }

    @Override
    public boolean retainAll(Collection<?> elements) {
        boolean retained = list.retainAll(elements);
        iChanged();
        return retained;
    }

    @Override
    public T set(int index, T element) {
        T previous = list.set(index, element);
        iChanged();
        return previous;
    }

    @Override
    public void sort(@Nullable Comparator<? super T> c) {
        list.sort(c);
        iChanged();
    }

    // ── pass-through ─────────────────────────────────────────────────────────
    // Note: mutations through iterator/listIterator/subList bypass change notification.

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object element) {
        return list.contains(element);
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        return list.containsAll(elements);
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(@Nullable Object element) {
        return list.indexOf(element);
    }

    @Override
    public int lastIndexOf(@Nullable Object element) {
        return list.lastIndexOf(element);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <A> A[] toArray(A[] array) {
        return list.toArray(array);
    }
}
