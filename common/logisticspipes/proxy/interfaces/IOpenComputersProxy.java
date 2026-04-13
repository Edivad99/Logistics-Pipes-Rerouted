package logisticspipes.proxy.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.opencomputers.IOCTile;

public interface IOpenComputersProxy {

	void initLogisticsTileGenericPipe(LogisticsTileGenericPipe tile);

	void initLogisticsSolidTileEntity(LogisticsSolidTileEntity tile);

	void addToNetwork(BlockEntity tile);

	void handleInvalidate(IOCTile tile);

	void handleChunkUnload(IOCTile tile);

	void handleWriteToNBT(IOCTile tile, CompoundTag nbt);

	void handleReadFromNBT(IOCTile tile, CompoundTag nbt);
}
