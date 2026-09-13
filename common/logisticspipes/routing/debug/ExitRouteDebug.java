package logisticspipes.routing.debug;

import java.util.List;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

public class ExitRouteDebug {

	public @Nullable List<BlockPos> filterPosition = null;
	public @Nullable String toStringNetwork = null;
	public boolean isNewlyAddedCanidate = true;
	public boolean isTraced = true;
	public int index = -1;
}
