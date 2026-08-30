package logisticspipes.interfaces;

import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.IPositionRotateble;

public interface ITubeOrientation {

	ITubeRenderOrientation getRenderOrientation();

	void rotatePositions(IPositionRotateble set);

	DoubleCoordinates getOffset();

	void setOnPipe(CoreMultiBlockPipe pipe);
}
