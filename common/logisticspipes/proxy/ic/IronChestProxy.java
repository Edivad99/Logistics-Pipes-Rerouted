package logisticspipes.proxy.ic;
// TODO: IronChest not ported to 1.20.1 — stub
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import logisticspipes.proxy.interfaces.IIronChestProxy;
public class IronChestProxy implements IIronChestProxy {
    @Override public boolean isIronChest(BlockEntity tile) { return false; }
    @Override @OnlyIn(Dist.CLIENT) public boolean isChestGui(Screen gui) { return false; }
}
