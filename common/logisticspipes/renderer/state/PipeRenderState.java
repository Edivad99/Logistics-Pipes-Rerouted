package logisticspipes.renderer.state;

import java.util.Arrays;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import logisticspipes.interfaces.IClientState;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter; // was BlockGetter
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class PipeRenderState implements IClientState {

	public enum LocalCacheType {
		QUADS
	}

	public final ConnectionMatrix pipeConnectionMatrix = new ConnectionMatrix();
	public final TextureMatrix textureMatrix = new TextureMatrix();

	public Cache<LocalCacheType, Object> objectCache = CacheBuilder.newBuilder().build();
	private boolean[] solidSidesCache = new boolean[6];
	private boolean savedStateHasMCMultiParts = false;

	private boolean dirty = true;

	public PipeRenderState() {
	}

	public void clean() {
		dirty = false;
		pipeConnectionMatrix.clean();
		textureMatrix.clean();
		clearRenderCaches();
	}

	public boolean isDirty() {
		return dirty || pipeConnectionMatrix.isDirty() || textureMatrix.isDirty();
	}

	public boolean needsRenderUpdate() {
		return pipeConnectionMatrix.isDirty() || textureMatrix.isDirty();
	}

	public void checkForRenderUpdate(BlockGetter worldIn, BlockPos blockPos) {
		boolean[] solidSides = new boolean[6];
		for (Direction dir : Direction.values()) {
			DoubleCoordinates pos = CoordinateUtils.add(new DoubleCoordinates(blockPos), dir);
			BlockState blockSide = pos.getBlockState(worldIn);
			if (blockSide.isFaceSturdy(worldIn, pos.getBlockPos(), dir.getOpposite()) && !pipeConnectionMatrix.isConnected(dir)) {
				solidSides[dir.ordinal()] = true;
			}
		}
		boolean changed = false;
		if (!Arrays.equals(solidSides, solidSidesCache)) {
			solidSidesCache = solidSides.clone();
			clearRenderCaches();
			changed = true;
		}
		DoubleCoordinates pos = new DoubleCoordinates(blockPos);
		BlockEntity tile = pos.getTileEntity(worldIn);
		if (tile instanceof LogisticsTileGenericPipe) {
			// MCMultiPart not available on 1.20.1 — hasParts is always false (former dummy behavior).
			boolean hasParts = false;
			if (savedStateHasMCMultiParts != hasParts) {
				savedStateHasMCMultiParts = hasParts;
				clearRenderCaches();
				changed = true;
			}
		}
		if (changed && tile != null) {
			// Which mount brackets a pipe grows depends only on which neighbouring faces are
			// solid and unconnected, and that changes without any pipe state changing — placing
			// a stone block beside a pipe touches neither the connection nor the texture matrix.
			// So afterStateUpdated() never fires and never refreshes the ModelData the baked
			// model reads its PipeGeometryKey from: without the refresh below the cached key
			// keeps its old solid-side mask, and the mount appears only when something
			// unrelated happens to dirty the pipe. sendBlockUpdated re-meshes the section,
			// which matters when the pipe and the changed neighbour are in different sections.
			tile.requestModelDataUpdate();
			if (worldIn instanceof Level level) {
				BlockState state = level.getBlockState(blockPos);
				level.sendBlockUpdated(blockPos, state, state, 3);
			}
		}
	}

	public void clearRenderCaches() {
		objectCache.invalidateAll();
		objectCache.cleanUp();
	}

	@Override
	public void writeData(LPDataOutput output) {
		pipeConnectionMatrix.writeData(output);
		textureMatrix.writeData(output);
	}

	@Override
	public void readData(LPDataInput input) {
		pipeConnectionMatrix.readData(input);
		textureMatrix.readData(input);
	}
}
