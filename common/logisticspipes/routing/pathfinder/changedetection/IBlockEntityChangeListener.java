package logisticspipes.routing.pathfinder.changedetection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;


public interface IBlockEntityChangeListener {

	void pipeRemoved(BlockPos pos);

	void pipeAdded(BlockPos pos, Direction side);

}
