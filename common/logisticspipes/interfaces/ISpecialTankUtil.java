package logisticspipes.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.util.ITankUtil;

public interface ISpecialTankUtil extends ITankUtil {

	BlockEntity getTileEntity();

	ISpecialTankAccessHandler getSpecialHandler();
}
