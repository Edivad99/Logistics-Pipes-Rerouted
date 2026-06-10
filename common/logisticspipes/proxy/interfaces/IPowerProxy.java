package logisticspipes.proxy.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

public interface IPowerProxy {

	boolean isEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyReceiver getEnergyReceiver(BlockEntity tile, Direction face);

	ICoFHEnergyStorage getEnergyStorage(int i);

	boolean isAvailable();
}
