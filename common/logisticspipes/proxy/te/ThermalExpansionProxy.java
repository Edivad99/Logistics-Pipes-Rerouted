package logisticspipes.proxy.te;
// TODO: ThermalExpansion not ported to 1.20.1 — stub
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import logisticspipes.proxy.interfaces.IThermalExpansionProxy;
import logisticspipes.recipes.CraftingParts;
public class ThermalExpansionProxy implements IThermalExpansionProxy {
    @Override public boolean isTE() { return false; }
    @Override public CraftingParts getRecipeParts() { return null; }
    @Override public boolean isToolHammer(Item stack) { return false; }
    @Override public boolean canHammer(@Nonnull ItemStack stack, Player player, BlockPos pos) { return false; }
    @Override public void toolUsed(@Nonnull ItemStack stack, Player player, BlockPos pos) {}
}
