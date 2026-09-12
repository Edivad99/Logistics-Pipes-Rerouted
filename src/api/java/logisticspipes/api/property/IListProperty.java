package logisticspipes.api.property;

import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IListProperty<T> extends List<T>, Property<List<T>> {

    /** Grows the list to {@code size}, filling new slots from {@code fillWith}. */
    void ensureSize(int size, IntFunction<T> fillWith);

    /** Grows the list to {@code size}, filling new slots with {@link ListProperty#defaultValue(int)}. */
    void ensureSize(int size);

    /** @return true when the content actually differed and was replaced. */
    boolean replaceContent(Collection<? extends T> col);

    boolean replaceContent(T[] arr);

    T readSingleFromNBT(ValueInput input, String key);

    void writeSingleToNBT(ValueOutput output, String key, T value);

    T copyValue(T obj);
}
