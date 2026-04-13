package crazypants.enderio.conduit;

import net.minecraft.core.Direction;

import com.enderio.core.common.util.BlockCoord;

public interface IConduit {

	ConnectionMode getConnectionMode(Direction dir);

	BlockCoord getLocation();
}
