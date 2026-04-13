package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import logisticspipes.api.ILPPipeConfigTool;
import logisticspipes.api.ILPPipeTile;

public class ItemPipeManager extends LogisticsItem implements ILPPipeConfigTool {

	public ItemPipeManager() {
		super();
	}

	@Override
	public boolean canWrench(Player player, @Nonnull ItemStack wrench, ILPPipeTile pipe) {
		return true;
	}

	@Override
	public void wrenchUsed(Player player, @Nonnull ItemStack wrench, ILPPipeTile pipe) {}

	@Override
	public boolean doesSneakBypassUse(@Nonnull ItemStack stack, net.minecraft.world.level.LevelReader world, BlockPos pos, Player player) {
		return true;
	}
}
