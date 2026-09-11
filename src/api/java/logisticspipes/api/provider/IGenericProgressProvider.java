package logisticspipes.api.provider;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Reads how far along a machine is, for a machine whose class you do not own.
 *
 * <p>Where {@link IProgressProvider} is implemented by the block entity itself, this is a separate
 * reader asked about someone else's -- how an addon teaches Logistics Pipes to understand a machine
 * from another mod, or from vanilla.
 *
 * <p>Register one from {@link logisticspipes.api.event.RegisterProgressProvidersEvent}.
 */
public interface IGenericProgressProvider {

    /** Whether this reader understands {@code blockEntity}. */
    boolean isType(BlockEntity blockEntity);

    /** @return how far along, from 0 to 100. Only asked when {@link #isType} said yes. */
    byte getProgress(BlockEntity blockEntity);
}
