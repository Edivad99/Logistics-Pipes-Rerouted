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
import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;

public class ClientRouter implements IRouter {

	private final UUID id;
	private final Identifier dimension;
    private final BlockPos pos;

	public ClientRouter(@Nullable UUID id, Identifier dimension, BlockPos pos) {
		this.id = id != null ? id : UUID.randomUUID();
		this.dimension = dimension;
		this.pos = pos;
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
	public @Nullable ExitRoute getExitFor(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return null;
	}

	@Override
	public List<@Nullable List<ExitRoute>> getRouteTable() {
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
        if (level.getBlockEntity(this.pos) instanceof LogisticsTileGenericPipe pipe) {
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
		return this.dimension.equals(dimension);
	}

	@Override
	public boolean isAt(Identifier dimension, BlockPos pos) {
		return this.dimension.equals(dimension) && this.pos.equals(pos);
	}

	@Override
	public BlockPos getPos() {
		return this.pos;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
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
		return List.of();
	}

	@Override
	public boolean isSideDisconnected(Direction dir) {
		return false;
	}

	@Override
	public List<ExitRoute> getDistanceTo(IRouter r) {
		return List.of();
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
		return List.of();
	}

	@Override
	public String toString() {
		return String.format("ClientRouter: {UUID: %s, AT: (%s)}", getId(), pos);
	}

	@Override
	public List<ExitRoute> getRoutersOnSide(Direction exitOrientation) {
		return List.of();
	}

	@Override
	public int getDistanceToNextPowerPipe(Direction dir) {
		return 0;
	}

	@Override
	public void queueTask(int i, IRouterQueuedTask callable) {}
}
