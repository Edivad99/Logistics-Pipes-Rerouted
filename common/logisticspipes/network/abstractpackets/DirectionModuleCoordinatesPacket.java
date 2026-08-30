package logisticspipes.network.abstractpackets;

import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public abstract class DirectionModuleCoordinatesPacket extends ModuleCoordinatesPacket {
	private @Nullable Direction direction;

	public DirectionModuleCoordinatesPacket(int id) {
		super(id);
	}

	public DirectionModuleCoordinatesPacket setDirection(@Nullable Direction newDirection) {
		direction = newDirection;
		return this;
	}

	public @Nullable Direction getDirection() {
		return direction;
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeFacing(direction);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		direction = input.readFacing();
	}
}
