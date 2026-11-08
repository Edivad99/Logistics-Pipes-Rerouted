package logisticspipes.integrations.buildcraft;

import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.api.provider.IGenericProgressProvider;

/**
 * Reads how far a BuildCraft Auto Workbench is into its next craft.
 *
 * <p>The workbench crafts once it has stored enough power, so its progress is that power as a share
 * of what one craft takes.
 */
public class AutoWorkbenchProgressProvider implements IGenericProgressProvider {

    @Override
    public boolean isType(BlockEntity blockEntity) {
        return AutoWorkbench.isReadable(blockEntity);
    }

    @Override
    public byte getProgress(BlockEntity blockEntity) {
        Double progress = AutoWorkbench.read(() -> AutoWorkbench.progress(blockEntity));
        if (progress == null) {
            return 0;
        }
        return (byte) Math.clamp(Math.round(progress * 100), 0, 100);
    }
}
