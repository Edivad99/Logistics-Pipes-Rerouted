package logisticspipes.inventory.tank;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.util.ITankUtil;

public interface ISpecialTankUtil extends ITankUtil {

    BlockEntity getBlockEntity();

    ISpecialTankAccessHandler getSpecialHandler();
}
