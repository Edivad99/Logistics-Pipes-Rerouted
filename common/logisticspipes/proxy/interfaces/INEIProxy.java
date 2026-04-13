package logisticspipes.proxy.interfaces;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.HitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface INEIProxy {

	@Nonnull
	ItemStack getItemForPosition(Level world, Player player, HitResult objectMouseOver);

	List<String> getInfoForPosition(Level world, Player player, HitResult objectMouseOver);

	@OnlyIn(Dist.CLIENT)
	boolean renderItemToolTip(int posX, int posY, List<String> msg, ChatFormatting rarityColor, @Nonnull ItemStack stack);

	@OnlyIn(Dist.CLIENT)
	List<String> getItemToolTip(@Nonnull ItemStack var22, Player thePlayer, TooltipFlag advancedItemTooltips, AbstractContainerScreen screen);
}
