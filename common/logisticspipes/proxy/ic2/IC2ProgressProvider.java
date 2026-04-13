package logisticspipes.proxy.ic2;
// TODO: IC2 not ported to 1.20.1 — stub
import net.minecraft.world.level.block.entity.BlockEntity;
import logisticspipes.proxy.interfaces.IGenericProgressProvider;
public class IC2ProgressProvider implements IGenericProgressProvider {
    @Override public boolean isType(BlockEntity tile) { return false; }
    @Override public byte getProgress(BlockEntity tile) { return 0; }
}
