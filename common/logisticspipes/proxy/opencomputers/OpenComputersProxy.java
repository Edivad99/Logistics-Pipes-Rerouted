package logisticspipes.proxy.opencomputers;
// TODO: OpenComputers not ported to 1.20.1 — stub
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.interfaces.IOpenComputersProxy;
public class OpenComputersProxy implements IOpenComputersProxy {
    @Override public void initLogisticsTileGenericPipe(LogisticsTileGenericPipe tile) {}
    @Override public void initLogisticsSolidTileEntity(LogisticsSolidTileEntity tile) {}
    @Override public void addToNetwork(BlockEntity tile) {}
    @Override public void handleInvalidate(IOCTile tile) {}
    @Override public void handleChunkUnload(IOCTile tile) {}
    @Override public void handleWriteToNBT(IOCTile tile, CompoundTag nbt) {}
    @Override public void handleReadFromNBT(IOCTile tile, CompoundTag nbt) {}
}
