package logisticspipes.proxy.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import org.jspecify.annotations.Nullable;

public interface IPowerProxy {

	boolean isEnergyReceiver(BlockEntity tile, Direction face);

	/**
     * The neighbour's energy capability, or null when it has none or will not take energy.
     */
	@Nullable EnergyHandler getEnergyReceiver(BlockEntity blockEntity, Direction face);

	boolean isAvailable();
}
