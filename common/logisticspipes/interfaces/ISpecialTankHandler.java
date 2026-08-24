package logisticspipes.interfaces;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

public interface ISpecialTankHandler {

	boolean init();

	boolean isType(@Nullable BlockEntity blockEntity);

	List<BlockEntity> getBaseTilesFor(BlockEntity blockEntity);
}
