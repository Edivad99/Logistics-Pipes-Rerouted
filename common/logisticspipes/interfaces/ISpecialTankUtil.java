package logisticspipes.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface ISpecialTankUtil extends ITankUtil {

	BlockEntity getTileEntity();

	ISpecialTankAccessHandler getSpecialHandler();
}
