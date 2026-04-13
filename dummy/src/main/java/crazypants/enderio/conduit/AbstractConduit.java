package crazypants.enderio.conduit;

import net.minecraft.core.Direction;

import com.enderio.core.common.util.BlockCoord;

public class AbstractConduit implements IConduit {

	@Override
	public ConnectionMode getConnectionMode(Direction dir) {
		return null;
	}

	@Override
	public BlockCoord getLocation() {
		return null;
	}
}
