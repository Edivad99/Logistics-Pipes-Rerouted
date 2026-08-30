package logisticspipes.asm.te;

import net.minecraft.core.Direction;

import logisticspipes.util.DoubleCoordinates;

public interface ITileEntityChangeListener {

	void pipeRemoved(DoubleCoordinates pos);

	void pipeAdded(DoubleCoordinates pos, Direction side);

}
