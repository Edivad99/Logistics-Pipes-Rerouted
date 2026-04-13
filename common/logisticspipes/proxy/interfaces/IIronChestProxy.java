package logisticspipes.proxy.interfaces;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IIronChestProxy {

	boolean isIronChest(BlockEntity tile);

	@OnlyIn(Dist.CLIENT)
	boolean isChestGui(Screen gui);
}
