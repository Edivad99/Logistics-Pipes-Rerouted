package logisticspipes.proxy.td.subproxies;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ITDPart {

	BlockEntity getInternalDuct();

	void setWorld_LP(Level world);

	void invalidate();

	void onChunkUnload();

	void scheduleNeighborChange();

	void connectionsChanged();

	boolean isLPSideBlocked(int i);

	void setPos(BlockPos pos);
}