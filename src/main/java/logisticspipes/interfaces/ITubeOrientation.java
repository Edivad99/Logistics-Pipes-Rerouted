package logisticspipes.interfaces;

import net.minecraft.core.BlockPos;

import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.utils.IPositionRotateble;

/**
 * How a multiblock tube is laid out.
 */
public interface ITubeOrientation {

    ITubeRenderOrientation getRenderOrientation();

    /**
     * Turns the given positions to this orientation.
     */
    void rotatePositions(IPositionRotateble positions);

    /**
     * @return where the tube goes, relative to the position it is being placed at
     */
    BlockPos getOffset();

    void setOnPipe(CoreMultiBlockPipe pipe);
}
