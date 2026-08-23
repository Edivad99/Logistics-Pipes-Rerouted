package logisticspipes.proxy;

import javax.annotation.Nullable;

import logisticspipes.proxy.interfaces.IPowerProxy;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Finds the energy capability on a neighbouring block.
 *
 * <p>Used to carry two abstractions of its own, {@code ICoFHEnergyStorage} and
 * {@code ICoFHEnergyReceiver}, left over from when LP spoke to CoFH's energy API directly and later
 * to NeoForge's {@code IEnergyStorage}. Both are gone: the 21.9 transfer rework's
 * {@link EnergyHandler} is the same shape, so there is nothing left to wrap and this class is down
 * to looking the capability up.</p>
 */
public class PowerProxy implements IPowerProxy {

	@Override
	public boolean isEnergyReceiver(BlockEntity tile, Direction face) {
		// Presence only. The callers follow this with getEnergyReceiver, which does the real test;
		// probing here too would open two throwaway transactions per neighbour per tick.
		return tile != null && tile.getLevel() != null
				&& tile.getLevel().getCapability(Capabilities.Energy.BLOCK, tile.getBlockPos(), face) != null;
	}

	@Override
	public @Nullable EnergyHandler getEnergyReceiver(BlockEntity blockEntity, Direction face) {
		if (blockEntity == null || blockEntity.getLevel() == null) {
			return null;
		}
		EnergyHandler handler = blockEntity.getLevel().getCapability(Capabilities.Energy.BLOCK, blockEntity.getBlockPos(), face);
		if (handler == null) {
			return null;
		}
		// The old code asked canReceive(); the transfer API has no such flag, so ask the question
		// the flag stood in for -- would an insert do anything -- and roll it back. A neighbour
		// that is merely full answers no, but its need works out to zero anyway, so the callers
		// that weigh needs against each other are unaffected.
		try (Transaction transaction = Transaction.openRoot()) {
			return handler.insert(1, transaction) > 0 ? handler : null;
		}
	}

	@Override
	public boolean isAvailable() {
		return true;
	}
}
