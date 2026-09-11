package logisticspipes.proxy.progressprovider;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.provider.IGenericProgressProvider;

public class FurnaceProgressProvider implements IGenericProgressProvider {

    @Override
    public boolean isType(BlockEntity blockEntity) {
        return blockEntity instanceof AbstractFurnaceBlockEntity;
    }

    @Override
    public byte getProgress(BlockEntity blockEntity) {
        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            int total = furnace.cookingTotalTime;
            if (total > 0) {
                return (byte) Math.clamp(furnace.cookingTimer * 100L / total, 0, 100);
            }
        }
        return 0;
    }
}
