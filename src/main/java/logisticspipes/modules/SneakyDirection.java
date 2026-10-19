package logisticspipes.modules;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

/**
 * A module that pulls items from one particular side of the inventory it is attached to.
 *
 * <p>Null means the default side, the one the pipe itself points at.
 */
public interface SneakyDirection {

    @Nullable Direction getSneakyDirection();

    void setSneakyDirection(@Nullable Direction direction);
}
