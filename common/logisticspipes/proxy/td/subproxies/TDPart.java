package logisticspipes.proxy.td.subproxies;
// TODO: ThermalDynamics not ported to 1.20.1 — stub
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
public class TDPart implements ITDPart {
    @Override public BlockEntity getInternalDuct() { return null; }
    @Override public void setWorld_LP(Level world) {}
    @Override public void invalidate() {}
    @Override public void onChunkUnload() {}
    @Override public void scheduleNeighborChange() {}
    @Override public void connectionsChanged() {}
    @Override public boolean isLPSideBlocked(int i) { return false; }
    @Override public void setPos(BlockPos pos) {}
}
