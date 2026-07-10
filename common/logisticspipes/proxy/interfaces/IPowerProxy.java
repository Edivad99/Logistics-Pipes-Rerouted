package logisticspipes.proxy.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IPowerProxy {

	boolean isEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyReceiver getEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyStorage getEnergyStorage(int i);

	boolean isAvailable();
}
