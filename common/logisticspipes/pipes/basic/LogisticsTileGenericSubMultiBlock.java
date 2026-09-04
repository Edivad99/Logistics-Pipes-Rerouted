package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;

import logisticspipes.interfaces.ITickable;
import logisticspipes.network.UpdateTagPayload;
import logisticspipes.network.to_client.block.MultiBlockPositionMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.ISubMultiBlockPipeInformationProvider;
import logisticspipes.ticks.ClientTaskQueue;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.TileBuffer;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;

public class LogisticsTileGenericSubMultiBlock extends BlockEntity implements ISubMultiBlockPipeInformationProvider, ITickable {

	private Set<DoubleCoordinates> mainPipePos = new HashSet<>();
	private List<LogisticsTileGenericPipe> mainPipe;
	private List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes = new ArrayList<>();
	private TileBuffer[] tileBuffer;

	@Deprecated
	public LogisticsTileGenericSubMultiBlock(BlockPos blockPos, BlockState blockState) {
		super(LPBlockEntityTypes.SUB_PIPE.get(), blockPos, blockState);
	}

	public LogisticsTileGenericSubMultiBlock(BlockPos blockPos, BlockState blockState, DoubleCoordinates pos) {
		super(LPBlockEntityTypes.SUB_PIPE.get(), blockPos, blockState);
		if (pos != null) {
			mainPipePos.add(pos);
		}
		mainPipe = null;
	}

	/**
	 * Removing a sub block takes its main pipes down with it. Was
	 * {@code LogisticsBlockGenericSubMultiBlock#onRemove} before 1.21.5 split block removal into
	 * {@code preRemoveSideEffects} (block entity still attached) and
	 * {@code affectNeighborsAfterRemoval} (block entity already gone).
	 *
	 * <p>The old body also called {@code onRemove} on the pipe block with the same state for both
	 * the old and the new state, which its own {@code state.getBlock() != newState.getBlock()}
	 * guard always rejected; that call did nothing and has not been carried over. Re-entrancy is
	 * still handled by {@code redirectedToMainPipe}, which
	 * {@code LogisticsBlockGenericPipe#removePipe} raises while it clears sub blocks.</p>
	 */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (!LogisticsBlockGenericSubMultiBlock.redirectedToMainPipe && level != null) {
			getMainPipe().stream()
					.filter(Objects::nonNull)
					.filter(LogisticsTileGenericPipe::isMultiBlock)
					.forEach(mainPipe -> level.removeBlock(mainPipe.getBlockPos(), false));
		}
		super.preRemoveSideEffects(pos, state);
	}

	public List<LogisticsTileGenericPipe> getMainPipe() {
		if (mainPipe == null) {
			mainPipe = new ArrayList<>();
			for (DoubleCoordinates pos : mainPipePos) {
				BlockEntity tile = pos.getTileEntity(getLevel());
				if (tile instanceof LogisticsTileGenericPipe) {
					mainPipe.add((LogisticsTileGenericPipe) tile);
				}
			}
			mainPipe = Collections.unmodifiableList(mainPipe);
		}
		if (MainProxy.isServer(getLevel())) {
			boolean allInvalid = true;
			for (LogisticsTileGenericPipe pipe : mainPipe) {
				if (!pipe.isRemoved()) {
					allInvalid = false;
					break;
				}
			}
			if (mainPipe.isEmpty() || allInvalid) {
				getLevel().removeBlock(getBlockPos(), false);
			}
		}
		if (mainPipe != null) {
			return mainPipe;
		}
		return Collections.emptyList();
	}

	/**
	 * Side-effect-free variant of {@link #getMainPipe()} for shape/pick queries: no caching,
	 * and never mutates the world (getMainPipe may remove this block when all mains are gone,
	 * which must not happen mid collision query).
	 */
	public List<LogisticsTileGenericPipe> getConnectedMainPipes() {
		if (getLevel() == null) {
			return Collections.emptyList();
		}
		List<LogisticsTileGenericPipe> result = new ArrayList<>(mainPipePos.size());
		for (DoubleCoordinates pos : mainPipePos) {
			BlockEntity tile = pos.getTileEntity(getLevel());
			if (tile instanceof LogisticsTileGenericPipe) {
				result.add((LogisticsTileGenericPipe) tile);
			}
		}
		return result;
	}

	public List<CoreMultiBlockPipe.SubBlockTypeForShare> getSubTypes() {
		return Collections.unmodifiableList(subTypes);
	}

	@Override
	public void update() {
		if (MainProxy.isClient(getLevel())) {
			return;
		}
		List<LogisticsTileGenericPipe> pipes = getMainPipe();
		for (LogisticsTileGenericPipe pipe : pipes) {
			pipe.subMultiBlock.add(new DoubleCoordinates(this));
		}
	}

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
		DoubleCoordinates single = DoubleCoordinates.deserialize("MainPipePos_", input);
		if (single != null) {
			mainPipePos.clear();
			mainPipePos.add(single);
		}
		for (ValueInput entry : input.childrenListOrEmpty("MainPipePosList")) {
			DoubleCoordinates pos = DoubleCoordinates.deserialize("MainPipePos_", entry);
			if (pos != null) {
				mainPipePos.add(pos);
			}
		}
		input.list("SubTypeList", Codec.STRING).ifPresent(names -> {
			subTypes.clear();
			for (String name : names) {
				subTypes.add(CoreMultiBlockPipe.SubBlockTypeForShare.valueOf(name));
			}
		});
		mainPipe = null;
	}

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
		ValueOutput.ValueOutputList posList = output.childrenList("MainPipePosList");
		for (DoubleCoordinates pos : mainPipePos) {
			pos.serialize("MainPipePos_", posList.addChild());
		}
		ValueOutput.TypedOutputList<String> typeList = output.list("SubTypeList", Codec.STRING);
		for (CoreMultiBlockPipe.SubBlockTypeForShare type : subTypes) {
			if (type == null) continue;
			typeList.add(type.name());
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		UpdateTagPayload.write(nbt, MultiBlockPositionMessage.STREAM_CODEC, getDescriptionMessage());
		return nbt;
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		applyDescription(input);
		super.handleUpdateTag(input);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ValueInput input) {
		applyDescription(input);
	}

	private void applyDescription(ValueInput input) {
		MultiBlockPositionMessage message = UpdateTagPayload.read(input, MultiBlockPositionMessage.STREAM_CODEC);
		if (message != null) {
			ClientTaskQueue.add(() -> message.applyTo(this));
		}
	}

	public MultiBlockPositionMessage getDescriptionMessage() {
		return new MultiBlockPositionMessage(
				getBlockPos(),
				mainPipePos.stream().map(DoubleCoordinates::getBlockPos).collect(Collectors.toSet()),
				subTypes);
	}

	public void setPosition(Set<BlockPos> mainPipes, List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes) {
		mainPipePos = mainPipes.stream().map(DoubleCoordinates::new).collect(Collectors.toSet());
		this.subTypes = subTypes;
		mainPipe = null;
	}

	public BlockEntity getTile() {
		return this;
	}

	public BlockEntity getTile(Direction to) {
		return getTile(to, false);
	}

	public BlockEntity getTile(Direction to, boolean force) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			if (force) {
				cache[to.ordinal()].refresh();
			}
			return cache[to.ordinal()].getTile();
		} else {
			return null;
		}
	}

	public Block getBlock(Direction to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			return cache[to.ordinal()].getBlock();
		} else {
			return null;
		}
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null) {
			tileBuffer = TileBuffer.makeBuffer(getLevel(), getBlockPos(), true);
		}
		return tileBuffer;
	}

	// invalidate() removed from BlockEntity in 1.20.1 — use setRemoved()
	@Override
	public void setRemoved() {
		super.setRemoved();
		tileBuffer = null;
	}

	// validate() removed from BlockEntity in 1.20.1 — use onLoad()
	@Override
	public void onLoad() {
		super.onLoad();
		tileBuffer = null;
	}

	public void scheduleNeighborChange() {
		tileBuffer = null;
	}

	public void addSubTypeTo(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		if (type == null) throw new NullPointerException();
		subTypes.add(type);
	}

	public void addMultiBlockMainPos(DoubleCoordinates placeAt) {
		if (mainPipePos.add(placeAt)) {
			mainPipe = null;
		}
	}

	public boolean removeMainPipe(DoubleCoordinates doubleCoordinates) {
		mainPipePos.remove(doubleCoordinates);
		return mainPipePos.isEmpty();
	}

	public void removeSubType(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		subTypes.remove(type);
	}

	@Override
	public IPipeInformationProvider getMainTile() {
		List<LogisticsTileGenericPipe> mainTiles = this.getMainPipe();
		if (mainTiles.size() != 1) {
			return null;
		}
		return mainTiles.get(0);
	}
}
