package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logisticspipes.interfaces.ITickable;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.multiblock.MultiBlockCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.ISubMultiBlockPipeInformationProvider;
import logisticspipes.utils.TileBuffer;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import network.rs485.logisticspipes.world.DoubleCoordinates;

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
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("MainPipePos_xPos")) {
			mainPipePos.clear();
			DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", tag);
			if (pos != null) {
				mainPipePos.add(pos);
			}
		}
		if (tag.contains("MainPipePosList")) {
			ListTag list = tag.getList("MainPipePosList", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", list.getCompound(i));
				if (pos != null) {
					mainPipePos.add(pos);
				}
			}
		}
		if (tag.contains("SubTypeList")) {
			ListTag list = tag.getList("SubTypeList", Tag.TAG_STRING);
			subTypes.clear();
			for (int i = 0; i < list.size(); i++) {
				String name = list.getString(i);
				CoreMultiBlockPipe.SubBlockTypeForShare type = CoreMultiBlockPipe.SubBlockTypeForShare.valueOf(name);
				if (type != null) {
					subTypes.add(type);
				}
			}
		}
		mainPipe = null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ListTag nbtList = new ListTag();
		for (DoubleCoordinates pos : mainPipePos) {
			CompoundTag compound = new CompoundTag();
			pos.writeToNBT("MainPipePos_", compound);
			nbtList.add(compound);
		}
		tag.put("MainPipePosList", nbtList);
		ListTag nbtTypeList = new ListTag();
		for (CoreMultiBlockPipe.SubBlockTypeForShare type : subTypes) {
			if (type == null) continue;
			nbtTypeList.add(StringTag.valueOf(type.name()));
		}
		tag.put("SubTypeList", nbtTypeList);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nbt;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		PacketHandler.queueAndRemovePacketFromNBT(tag);
		super.handleUpdateTag(tag, lookupProvider);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
		if (pkt.getTag() != null) {
			PacketHandler.queueAndRemovePacketFromNBT(pkt.getTag());
		}
	}

	public ModernPacket getLPDescriptionPacket() {
		MultiBlockCoordinatesPacket packet = PacketHandler.getPacket(MultiBlockCoordinatesPacket.class);
		packet.setTilePos(this);
		packet.setTargetPos(mainPipePos);
		packet.setSubTypes(subTypes);
		return packet;
	}

	public void setPosition(Set<DoubleCoordinates> lpPosition, List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes) {
		mainPipePos = lpPosition;
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
