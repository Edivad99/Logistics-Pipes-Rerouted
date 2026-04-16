package logisticspipes.proxy.enderchest;
// NOTE: EnderStorage not ported to 1.20.1 — stub
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import logisticspipes.proxy.interfaces.IEnderStorageProxy;
public class EnderStorageProxy implements IEnderStorageProxy {
    @Override public boolean isEnderChestBlock(Block block) { return false; }
    @Override public void openEnderChest(Level world, int x, int y, int z, Player player) {}
}
