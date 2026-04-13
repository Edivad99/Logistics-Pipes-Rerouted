package logisticspipes.proxy.interfaces;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import logisticspipes.recipes.CraftingParts;

public interface IThermalExpansionProxy {

	boolean isTE();

	CraftingParts getRecipeParts();

	boolean isToolHammer(Item stack);

	boolean canHammer(@Nonnull ItemStack stack, Player entityplayer, BlockPos pos);

	void toolUsed(@Nonnull ItemStack stack, Player entityplayer, BlockPos pos);
}
