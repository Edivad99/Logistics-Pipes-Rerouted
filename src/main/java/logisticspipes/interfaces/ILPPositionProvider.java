package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;

/**
 * Something with a position in the level.
 */
public interface ILPPositionProvider {

    BlockPos getPos();
}
