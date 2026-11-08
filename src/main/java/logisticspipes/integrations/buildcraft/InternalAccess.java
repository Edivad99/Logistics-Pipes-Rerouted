package logisticspipes.integrations.buildcraft;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;

/**
 * Guards the reads of one BuildCraft block's internal classes.
 *
 * <p>BuildCraft's internals can change in any release. Every read goes through {@link #read}, and the
 * first {@link LinkageError} turns that block's support off with a single warning instead of crashing
 * the game.
 */
final class InternalAccess {

    private final String block;
    private boolean disabled;

    /**
     * @param block the block being read, as the warning names it
     */
    InternalAccess(String block) {
        this.block = block;
    }

    boolean isDisabled() {
        return disabled;
    }

    /**
     * Runs a read of BuildCraft's internals.
     *
     * @return what it read, or null when the internals no longer match and reading is now turned off
     */
    <T> @Nullable T read(Supplier<T> reader) {
        if (disabled) {
            return null;
        }
        try {
            return reader.get();
        } catch (LinkageError e) {
            disabled = true;
            LogisticsPipes.LOG.warn("This BuildCraft version changed {} in a way Logistics Pipes does not know; "
                + "reading it from Logistics Pipes is turned off", block, e);
            return null;
        }
    }
}
