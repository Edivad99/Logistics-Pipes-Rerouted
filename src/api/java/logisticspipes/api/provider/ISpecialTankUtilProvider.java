package logisticspipes.api.provider;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.util.ITankUtil;

/**
 * Supplies an {@link ITankUtil} for a block that holds fluids in a way the fluid handler capability
 * cannot express -- a storage network, where there are no tanks to enumerate.
 *
 * <p>Providers are asked before the capability lookup, so claiming a block means owning it: the
 * fallback no longer runs for it, and the provider has to serve every operation, not only the reads
 * it cares about.
 *
 * <p>Register through {@code RegisterTankHandlersEvent}.
 */
public interface ISpecialTankUtilProvider {

    /** Whether this provider claims the storage reachable at {@code dir} of {@code blockEntity}. */
    boolean isType(BlockEntity blockEntity, @Nullable Direction dir);

    /**
     * @return the way into that storage, or null to let the capability lookup run after all. Only
     *         asked when {@link #isType} said yes.
     */
    @Nullable
    ITankUtil getTankUtilFor(BlockEntity blockEntity, @Nullable Direction dir);
}
