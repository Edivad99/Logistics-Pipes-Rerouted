package logisticspipes.interfaces;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface ISpecialTankHandler {

	boolean init();

	boolean isType(@Nullable BlockEntity blockEntity);

	List<BlockEntity> getBaseTilesFor(BlockEntity blockEntity);
}
