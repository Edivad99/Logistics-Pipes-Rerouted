package logisticspipes.routing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class ClientRouter implements IRouter {

	private final int xCoord;
	private final int yCoord;
	private final int zCoord;

	public ClientRouter(UUID id, Identifier dimension, int xCoord, int yCoord, int zCoord) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
	}

	@Override
	public void destroy() {}

	@Override
	public int getSimpleID() {
		return -420;
	}

	@Override
	public void update(boolean doFullRefresh, CoreRoutedPipe pipe) {}

	@Override
	public boolean isRoutedExit(Direction connection) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return false;
	}

	@Override
	public boolean hasRoute(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return false;
	}

	@Override
	public ExitRoute getExitFor(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return null;
	}

	@Override
	public ArrayList<List<ExitRoute>> getRouteTable() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return new ArrayList<>();
	}

	@Override
	public List<ExitRoute> getIRoutersByCost() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return new LinkedList<>();
	}

	@Override
    @Nullable
	public CoreRoutedPipe getPipe() {
		Level level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
        if (level.getBlockEntity(new BlockPos(xCoord, yCoord, zCoord)) instanceof LogisticsTileGenericPipe pipe) {
            if (pipe.pipe instanceof CoreRoutedPipe coreRoutedPipe) {
                return coreRoutedPipe;
            }
        }
        return null;
    }

	@Override
	public @Nullable CoreRoutedPipe getCachedPipe() {
		return getPipe();
	}

	@Override
	public boolean isInDim(Identifier dimension) {
		return true;
	}

	@Override
	public boolean isAt(Identifier dimension, int xCoord, int yCoord, int zCoord) {
		return this.xCoord == xCoord && this.yCoord == yCoord && this.zCoord == zCoord;
	}

	@Override
	public DoubleCoordinates getLPPosition() {
		return new DoubleCoordinates(xCoord, yCoord, zCoord);
	}

	@Override
	public UUID getId() {
		return UUID.randomUUID();
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		CoreRoutedPipe pipe = getPipe();
		if (pipe == null) {
			return null;
		}
		return pipe.getLogisticsModule();
	}

	@Override
	public void clearPipeCache() {}

	@Override
	public List<Pair<ILogisticsPowerProvider, List<IFilter>>> getPowerProvider() {
		return null;
	}

	@Override
	public boolean isSideDisconnected(Direction dir) {
		return false;
	}

	@Override
	public List<ExitRoute> getDistanceTo(IRouter r) {
		return null;
	}

	@Override
	public void clearInterests() {}

	@Override
	public boolean isCacheInvalid() {
		return false;
	}

	@Override
	public void forceLsaUpdate() {}

	@Override
	public boolean isSubPoweredExit(Direction connection) {
		return false;
	}

	@Override
	public List<Pair<ISubSystemPowerProvider, List<IFilter>>> getSubSystemPowerProvider() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("ServerRouter: {UUID: %s, AT: (%d, %d, %d)}", getId(), xCoord, yCoord, zCoord);
	}

	@Override
	public List<ExitRoute> getRoutersOnSide(Direction exitOrientation) {
		return null;
	}

	@Override
	public int getDistanceToNextPowerPipe(Direction dir) {
		return 0;
	}

	@Override
	public void queueTask(int i, IRouterQueuedTask callable) {}
}
