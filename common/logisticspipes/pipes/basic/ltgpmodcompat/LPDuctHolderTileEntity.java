package logisticspipes.pipes.basic.ltgpmodcompat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import logisticspipes.proxy.td.subproxies.ITDPart;

/**
 * Intermediate BlockEntity base for LP pipe tile entities.
 * ThermalDynamics integration (IDuctHolder) was removed for 1.20.1 — no 1.20.1 port exists.
 * Previously implemented cofh.thermaldynamics.duct.tiles.IDuctHolder.
 */
public abstract class LPDuctHolderTileEntity extends LPMicroblockTileEntity {

	public ITDPart tdPart;

	public LPDuctHolderTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
