package logisticspipes.pipes.signs;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;

/**
 * The kinds of pipe sign, in the order the sign creator cycles through them.
 *
 * <p>The ordinal is the id saved with the pipe and sent to clients: never reorder these, only add
 * at the end.</p>
 */
public enum PipeSignType {
    CRAFTING(pipe -> pipe instanceof PipeItemsCraftingLogistics,
        (pipe, _) -> new CraftingPipeSign((PipeItemsCraftingLogistics) pipe)),
    ITEM_AMOUNT(_ -> true, ItemAmountPipeSign::new);

    private final Predicate<CoreRoutedPipe> allowedOn;
    private final BiFunction<CoreRoutedPipe, Direction, IPipeSign> factory;

    PipeSignType(Predicate<CoreRoutedPipe> allowedOn, BiFunction<CoreRoutedPipe, Direction, IPipeSign> factory) {
        this.allowedOn = allowedOn;
        this.factory = factory;
    }

    public boolean isAllowedFor(CoreRoutedPipe pipe) {
        return allowedOn.test(pipe);
    }

    /**
     * A new sign on side {@code dir} of {@code pipe}, which {@link #isAllowedFor} must accept.
     */
    public IPipeSign create(CoreRoutedPipe pipe, Direction dir) {
        return factory.apply(pipe, dir);
    }

    /**
     * The type saved as {@code id}, or null when there is none.
     */
    public static @Nullable PipeSignType byId(int id) {
        PipeSignType[] types = values();
        return id >= 0 && id < types.length ? types[id] : null;
    }
}
