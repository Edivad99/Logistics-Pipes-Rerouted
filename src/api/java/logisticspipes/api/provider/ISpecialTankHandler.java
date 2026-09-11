package logisticspipes.api.provider;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * Splits one block into the several that actually hold the fluid -- a multiblock tank, where the
 * block a pipe touches is a wall of a structure stored elsewhere.
 *
 * <p>Register through {@code RegisterTankHandlersEvent}.
 */
public interface ISpecialTankHandler {

    /** Whether this handler understands {@code blockEntity}. */
    boolean isType(@Nullable BlockEntity blockEntity);

    /** The blocks holding the fluid. Only asked when {@link #isType} said yes. */
    List<BlockEntity> getBaseTilesFor(BlockEntity blockEntity);
}
