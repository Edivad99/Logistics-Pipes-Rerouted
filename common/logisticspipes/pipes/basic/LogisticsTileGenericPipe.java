package logisticspipes.pipes.basic;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILPPipe;
import logisticspipes.api.ILPPipeTile;
import logisticspipes.asm.ModDependentField;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.asm.te.LPTileEntityObject;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;
import logisticspipes.interfaces.IClientState;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.logic.LogicController;
import logisticspipes.logic.interfaces.ILogicControllerTile;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.block.PipeSolidSideCheck;
import logisticspipes.network.packets.pipe.PipeTileStatePacket;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.basic.ltgpmodcompat.LPMicroblockTileEntity;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.IIconProvider;
import logisticspipes.renderer.LogisticsTileRenderController;
import logisticspipes.renderer.state.PipeRenderState;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.changedetection.TEControl;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.StackTraceUtil;
import logisticspipes.utils.StackTraceUtil.Info;
import logisticspipes.utils.TileBuffer;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import lombok.Getter;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.connection.PipeInventoryConnectionChecker;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsTileGenericPipe extends LPMicroblockTileEntity
		implements ILPPipeTile, IPipeInformationProvider, /*IItemDuct,*/
		// ManagedPeripheral, Environment, SidedEnvironment — added at runtime by @ModDependentInterface ASM when OC is present
		ILogicControllerTile, ILPTEInformation, logisticspipes.interfaces.ITickable {

	// ILPTEInformation — previously injected by ASM, now implemented directly
    @Nullable
	private LPTileEntityObject lpTileEntityObject;

	@Override
	public @Nullable LPTileEntityObject getLPTileEntityObject() {
		return lpTileEntityObject;
	}

	@Override
	public void setLPTileEntityObject(LPTileEntityObject object) {
		this.lpTileEntityObject = object;
	}

	public static PipeInventoryConnectionChecker pipeInventoryConnectionChecker = new PipeInventoryConnectionChecker();

	private static final String NBT_PIPE_ID = "pipeIdName";

	public int statePacketId = 0;
	public final PipeRenderState renderState;
	public final CoreState coreState = new CoreState();
	public Object OPENPERIPHERAL_IGNORE; //Tell OpenPeripheral to ignore this class
	public Set<DoubleCoordinates> subMultiBlock = new HashSet<>();
	public boolean[] turtleConnect = new boolean[7];
	@ModDependentField(modId = LPConstants.computerCraftModID)
	public HashMap<Object, Direction> connections; // IComputerAccess — CC not ported
	@ModDependentField(modId = LPConstants.computerCraftModID)
	public Object currentPC; // IComputerAccess — CC not ported
	@ModDependentField(modId = LPConstants.openComputersModID)
	public Object node; // was: Node (OC removed from classpath)
	public LogicController logicController = new LogicController();
	public boolean[] pipeConnectionsBuffer = new boolean[6];
	public boolean[] pipeBCConnectionsBuffer = new boolean[6];
	public boolean[] pipeTDConnectionsBuffer = new boolean[6];
    @Nullable
	public CoreUnroutedPipe pipe;
	private LogisticsTileRenderController renderController;
	private boolean sendInitPacket = true;
	@Getter
	private boolean initialized = false;
	private boolean deletePipe = false;
    @Nullable
	private TileBuffer[] tileBuffer;
	private boolean sendClientUpdate = false;
	private boolean blockNeighborChange = false;
	private boolean refreshRenderState = false;
	private boolean pipeBound = false;
	private EnumMap<Direction, ItemInsertionHandler> itemInsertionHandlers;

	public LogisticsTileGenericPipe(BlockPos pos, BlockState state) {
		super(LPBlockEntityTypes.PIPE.get(), pos, state);
		itemInsertionHandlers = new EnumMap<>(Direction.class);
		Arrays.stream(Direction.values()).forEach(face -> itemInsertionHandlers.put(face, new ItemInsertionHandler(this, face)));
		ItemInsertionHandler itemInsertionHandlerNull = new ItemInsertionHandler(this, null);
		renderState = new PipeRenderState();
	}

	@Override
	public void setRemoved() {
		if (pipe == null) {
            initialized = false;
			tileBuffer = null;
			super.setRemoved();
		} else if (!pipe.preventRemove()) {
            initialized = false;
			tileBuffer = null;
			pipe.invalidate();
			super.setRemoved();
			TEControl.invalidate(this);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		initialized = false;
		tileBuffer = null;
		bindPipe();
		if (pipe != null) {
			pipe.validate();
		}
		TEControl.validate(this);
	}

	// onChunkUnload() removed in 1.20.1 — call manually if needed, or hook level unload events
	public void onChunkUnload() {
		if (pipe != null) {
			pipe.onChunkUnload();
		}
	}

	// Ticked via BlockEntityTicker in LogisticsBlockGenericPipe (ITickable.update)
	public void update() {
		final Info superDebug = StackTraceUtil.addSuperTraceInformation(() -> "Time: " + getLevel().getGameTime());
		final Info debug = StackTraceUtil.addTraceInformation(() -> "(" + getX() + ", " + getY() + ", " + getZ() + ")", superDebug);
		if (sendInitPacket && MainProxy.isServer(getLevel())) {
			sendInitPacket = false;
			getRenderController().sendInit();
		}
		if (!level.isClientSide) {
			if (deletePipe) {
				level.removeBlock(getBlockPos(), false);
			}

			if (pipe == null) {
				debug.end();
				return;
			}

			if (!initialized) {
				initialize(pipe);
			}
		}

		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			debug.end();
			return;
		}

		pipe.updateEntity();

		if (level.isClientSide) {
			debug.end();
			return;
		}

		if (blockNeighborChange) {
			computeConnections();
			pipe.onNeighborBlockChange();
			blockNeighborChange = false;
			refreshRenderState = true;

			if (MainProxy.isServer(level)) {
				MainProxy.sendPacketToAllWatchingChunk(this, PacketHandler.getPacket(PipeSolidSideCheck.class).setTilePos(this));
			}
		}

		//Sideblocks need to be checked before this
		//Network needs to be after this

		if (refreshRenderState) {
			refreshRenderState();

			if (renderState.isDirty()) {
				renderState.clean();
				sendUpdateToClient();
			}

			refreshRenderState = false;
		}

		if (sendClientUpdate) {
			sendClientUpdate = false;
			MainProxy.sendPacketToAllWatchingChunk(this, getLPDescriptionPacket());
		}

		getRenderController().onUpdate();
		debug.end();
	}

	private void refreshRenderState() {
		// Pipe connections;
		for (Direction o : Direction.values()) {
			renderState.pipeConnectionMatrix.setConnected(o, pipeConnectionsBuffer[o.ordinal()]);
			renderState.pipeConnectionMatrix.setBCConnected(o, pipeBCConnectionsBuffer[o.ordinal()]);
			renderState.pipeConnectionMatrix.setTDConnected(o, pipeTDConnectionsBuffer[o.ordinal()]);
		}
		// Pipe Textures
		for (int i = 0; i < 7; i++) {
			Direction o = Direction.from3DDataValue(i);
			renderState.textureMatrix.setIconIndex(o, pipe.getIconIndex(o));
		}
		//New Pipe Texture States
		renderState.textureMatrix.refreshStates(pipe);
	}

	@Override
	public boolean isMultipartAllowedInPipe() {
		return !isMultiBlock() && (pipe == null || pipe.isMultipartAllowedInPipe());
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		sendInitPacket = true;
		CompoundTag nbt = saveWithoutMetadata(registries);
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			LogisticsPipes.LOG.error("Failed to embed LP description packet in update tag at {}", getBlockPos(), e);
		}
		return nbt;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
		PacketHandler.queueAndRemovePacketFromNBT(tag);
		super.handleUpdateTag(tag, lookupProvider);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider lookupProvider) {
		CompoundTag nbt = packet.getTag();
        handleUpdateTag(nbt, lookupProvider);
    }

	// Custom public method — called from crash report hooks; not an override of BlockEntity or IBlockEntityExtension.
	public void addInfoToCrashReport(CrashReportCategory reportCategory) {
		if (pipe != null) {
			reportCategory.setDetail("Pipe", pipe.getClass().getCanonicalName());
			if (pipe.transport != null) {
				reportCategory.setDetail("Transport", pipe.transport.getClass().getCanonicalName());
			} else {
				reportCategory.setDetail("Transport", "null");
			}

			if (pipe instanceof CoreRoutedPipe) {
				try {
					((CoreRoutedPipe) pipe).addCrashReport(reportCategory);
				} catch (Exception e) {
					reportCategory.setDetail("Internal LogisticsPipes Error", e);
				}
			}
		}
	}

	public void scheduleNeighborChange() {
		if (MainProxy.isServer(level)) {
			pipe.triggerConnectionCheck();
		}
		blockNeighborChange = true;
		// ComputerCraft turtles cannot exist on 1.20.1 — no neighbor is ever a turtle.
		for (int i = 0; i < 6; i++) {
			turtleConnect[i] = false;
		}
	}

	/* IPipeInformationProvider */

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);

		if (pipe != null && pipe.item != null) {
			ResourceLocation key = BuiltInRegistries.ITEM.getKeyOrNull(pipe.item);
			if (key != null) {
				nbt.putString(NBT_PIPE_ID, key.toString());
			}
			pipe.writeToNBT(nbt, registries);
		} else if (coreState.pipeIdName != null) {
			nbt.putString(NBT_PIPE_ID, coreState.pipeIdName);
		}

		for (int i = 0; i < turtleConnect.length; i++) {
			nbt.putBoolean("turtleConnect_" + i, turtleConnect[i]);
		}

		CompoundTag logicNBT = new CompoundTag();
		logicController.writeToNBT(logicNBT, registries);
		nbt.put("logicController", logicNBT);
	}

	@Override
	protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		if (pipe != null) {
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			if (trace.length > 2 && trace[2].getMethodName().equals("handle") && trace[2].getClassName()
					.equals("com.xcompwiz.lookingglass.network.packet.PacketTileEntityNBT")) {
				LogisticsPipes.LOG.warn("Prevented false data injection by LookingGlass");
				return;
			}
		}
		super.loadAdditional(nbt, registries);

		if (!nbt.contains(NBT_PIPE_ID)) return;

		coreState.pipeIdName = nbt.getString(NBT_PIPE_ID);
		Item pipeItem = null;
		if (coreState.pipeIdName != null && !coreState.pipeIdName.isEmpty()) {
			pipeItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(coreState.pipeIdName));
		}
		pipe = LogisticsBlockGenericPipe.createPipe(pipeItem);
		// load() can run more than once on the client (initial chunk tag + later data packets).
		// Each run replaces the pipe object, so the bind must be redone or the fresh pipe keeps a
		// null container and the renderer NPEs in CoreRoutedPipe.isOpaque on a cold world load.
		pipeBound = false;
		bindPipe();

		if (pipe != null) {
			pipe.readFromNBT(nbt, registries);
			pipe.finishInit();
		} else {
			LogisticsPipes.LOG.warn("Pipe failed to load from NBT at {}", getBlockPos());
			deletePipe = true;
		}

		for (int i = 0; i < turtleConnect.length; i++) {
			turtleConnect[i] = nbt.getBoolean("turtleConnect_" + i);
		}

		logicController.readFromNBT(nbt.getCompound("logicController"), registries);
	}

	public boolean canPipeConnect(BlockEntity with, Direction side) {
		if (MainProxy.isClient(level)) {
			//XXX why is this ever called client side, its not *used* for anything.
			return false;
		}
		if (with == null) {
			return false;
		}

		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			return false;
		}

		if (with instanceof LogisticsTileGenericPipe) {
			CoreUnroutedPipe otherPipe = ((LogisticsTileGenericPipe) with).pipe;

			if (!(LogisticsBlockGenericPipe.isValid(otherPipe))) {
				return false;
			}

			if (!(otherPipe.canPipeConnect(this, side.getOpposite()))) {
				return false;
			}

		}
		return pipe.canPipeConnect(with, side);
	}

	// ComputerCraft is not available on 1.20.1 — these retain the former dummy-proxy semantics.
	public void queueEvent(String event, Object[] arguments) {}

	public void handleMesssage(int computerId, Object message, int sourceId) {}

	public boolean getTurtleConnect() {
		return false;
	}

	public void setTurtleConnect(boolean flag) {}

	public int getLastCCID() {
		return 0;
	}

	public ItemStack insertItem(Direction from, ItemStack stack) {
		int used = injectItem(stack, true, from);
		if (used == stack.getCount()) {
			return ItemStack.EMPTY;
		} else {
			stack = stack.copy();
			stack.shrink(used);
			return stack;
		}
	}

	public void addLaser(Direction dir, float length, int color, boolean reverse, boolean renderBall) {
		getRenderController().addLaser(dir, length, color, reverse, renderBall);
	}

	public void removeLaser(Direction dir, int color, boolean isBall) {
		getRenderController().removeLaser(dir, color, isBall);
	}

	public LogisticsTileRenderController getRenderController() {
		if (renderController == null) {
			renderController = new LogisticsTileRenderController(this);
		}
		return renderController;
	}

	@Override
	public boolean isCorrect(ConnectionType type) {
		return true;
	}

	@Override
	public int getX() {
		return getBlockPos().getX();
	}

	@Override
	public int getY() {
		return getBlockPos().getY();
	}

	@Override
	public int getZ() {
		return getBlockPos().getZ();
	}

	@Override
	public boolean isRouterInitialized() {
		return isInitialized() && (!isRoutingPipe() || !getRoutingPipe().stillNeedReplace());
	}

	@Override
	public boolean isRoutingPipe() {
		return pipe instanceof CoreRoutedPipe;
	}

	@Override
	public CoreRoutedPipe getRoutingPipe() {
		if (pipe instanceof CoreRoutedPipe) {
			return (CoreRoutedPipe) pipe;
		}
		throw new RuntimeException("This is no routing pipe");
	}

	@Override
	public boolean isFirewallPipe() {
		return pipe instanceof PipeItemsFirewall;
	}

	@Override
	public IFilter getFirewallFilter() {
		if (pipe instanceof PipeItemsFirewall) {
			return ((PipeItemsFirewall) pipe).getFilter();
		}
		throw new RuntimeException("This is no firewall pipe");
	}

	public BlockEntity getTile() {
		return this;
	}

	@Override
	public boolean divideNetwork() {
		return false;
	}

	@Override
	public boolean powerOnly() {
		return false;
	}

	@Override
	public boolean isOnewayPipe() {
		return false;
	}

	@Override
	public boolean isOutputClosed(Direction direction) {
		return false;
	}

	@Override
	public boolean isItemPipe() {
		return true;
	}

	@Override
	public boolean isFluidPipe() {
		return pipe != null && pipe.isFluidPipe();
	}

	@Override
	public boolean isPowerPipe() {
		return false;
	}

	@Override
	public boolean canConnect(BlockEntity to, Direction direction, boolean flag) {
		if (pipe == null) {
			return false;
		}
		return pipe.canPipeConnect(to, direction, flag);
	}

	@Override
	public double getDistance() {
		if (this.pipe != null && this.pipe.transport != null) {
			return this.pipe.transport.getPipeLength();
		}
		return 1;
	}

	@Override
	public double getDistanceWeight() {
		if (this.pipe != null && this.pipe.transport != null) {
			return this.pipe.transport.getDistanceWeight();
		}
		return 1;
	}

	public int injectItem(ItemStack payload, boolean doAdd, Direction from) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport != null && isPipeConnectedCached(from)) {
			if (doAdd && MainProxy.isServer(getLevel())) {
				ItemStack leftStack = payload.copy();
				int lastIterLeft;
				do {
					lastIterLeft = leftStack.getCount();
					LPTravelingItem.LPTravelingItemServer travelingItem = SimpleServiceLocator.routedItemHelper.createNewTravelItem(leftStack);
					leftStack.setCount(pipe.transport.injectItem(travelingItem, from.getOpposite()));
				} while (leftStack.getCount() != lastIterLeft && leftStack.getCount() != 0);
				return payload.getCount() - leftStack.getCount();
			}
		}
		return 0;
	}

	public boolean isPipeConnectedCached(Direction side) {
		if (MainProxy.isClient(this.level)) {
			return renderState.pipeConnectionMatrix.isConnected(side);
		} else {
			return pipeConnectionsBuffer[side.ordinal()];
		}
	}

	public boolean isOpaque() {
		return pipe.isOpaque();
	}

	// OC methods — @Override removed, types replaced with Object (OC not on classpath)
	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object node() {
		return node;
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onConnect(Object node1) {}
	//public int redstoneInput = 0;
	//public int[] redstoneInputSide = new int[Direction.values().length];

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onDisconnect(Object node1) {}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onMessage(Object message) {}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object[] invoke(String s, Object context, Object arguments) {
		// TODO: OC not available on classpath — BaseWrapperClass.WRAPPER and isDirectCall are OC-specific
		return new Object[0];
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public String[] methods() {
		return new String[] { "getPipe" };
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object sidedNode(Direction side) {
		if (this.getTile(side) instanceof LogisticsTileGenericPipe || this.getTile(side) instanceof LogisticsSolidBlockEntity) {
			return null;
		} else {
			return node();
		}
	}

	@OnlyIn(Dist.CLIENT)
	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public boolean canConnect(Direction side) {
		return !(this.getTile(side) instanceof LogisticsTileGenericPipe) && !(this.getTile(side) instanceof LogisticsSolidBlockEntity);
	}

	public void initialize(CoreUnroutedPipe pipe) {
		// blockType field removed in 1.20.1; use getBlockState().getBlock()
		
		if (pipe == null) {
			LogisticsPipes.LOG.warn("Pipe failed to initialize at " + getBlockPos().toString() + ", deleting");
			level.removeBlock(getBlockPos(), false);
			return;
		}

		this.pipe = pipe;

		/*
		for (Direction o : Direction.values()) {
			BlockEntity tile = getTile(o);

			if (tile instanceof LogisticsTileGenericPipe) {
				((LogisticsTileGenericPipe) tile).scheduleNeighborChange();
			}
		}*/

		bindPipe();

		computeConnections();
		scheduleRenderUpdate();

		if (pipe.needsInit()) {
			pipe.initialize();
		}

		initialized = true;
	}

	private void bindPipe() {
		if (!pipeBound && pipe != null) {
			pipe.setTile(this);
			if (pipe.item != null) {
				net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pipe.item);
				coreState.pipeIdName = key == null ? "" : key.toString();
			}
			pipeBound = true;
		}
	}

	/* SMP */

	public ModernPacket getLPDescriptionPacket() {
		bindPipe();
		// Ensure renderState carries fresh per-pipe data before snapshotting it into
		// the description packet — otherwise the initial chunk-send (via getUpdateTag)
		// transmits a default state and every pipe on the client renders with
		// textureIndex=0 and zero connections.
		if (pipe != null && level != null && !level.isClientSide) {
			computeConnections();
			refreshRenderState();
		}

		PipeTileStatePacket packet = PacketHandler.getPacket(PipeTileStatePacket.class);

		packet.setTilePos(this);

		packet.setCoreState(coreState);
		packet.setRenderState(renderState);
		packet.setPipe(pipe);
		packet.setStatePacketId(statePacketId++);

		return packet;
	}

	public void afterStateUpdated() {
		if (pipe == null && coreState.pipeIdName != null && !coreState.pipeIdName.isEmpty()) {
			Item pipeItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				ResourceLocation.parse(coreState.pipeIdName));
			initialize(LogisticsBlockGenericPipe.createPipe(pipeItem));
		}

		if (pipe == null) {
			return;
		}

		level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 3);

		if (renderState.needsRenderUpdate()) {
			level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 3);
			renderState.clean();
		}
	}

	public void sendUpdateToClient() {
		sendClientUpdate = true;
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null && pipe != null) {
			tileBuffer = TileBuffer.makeBuffer(this.level, this.worldPosition, pipe.transport.delveIntoUnloadedChunks());
		}
		return tileBuffer;
	}

	public void blockCreated(Direction from, Block block, BlockEntity tile) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			cache[from.getOpposite().ordinal()].set(block, tile);
		}
	}

	@Override
	public BlockEntity getNextConnectedTile(Direction to) {
		if (this.pipe.isMultiBlock()) {
			return ((CoreMultiBlockPipe) this.pipe).getConnectedEndTile(to);
		}
		return getTile(to, false);
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

	private void computeConnections() {
		TileBuffer[] cache = getTileCache();
		if (cache == null) {
			return;
		}

		for (Direction side : Direction.values()) {
			TileBuffer t = cache[side.ordinal()];
			t.refresh();

			pipeConnectionsBuffer[side.ordinal()] = canPipeConnect(t.getTile(), side);
			// BuildCraft / Thermal Dynamics do not exist on 1.20.1 — never a BC pipe or TD duct.
			pipeBCConnectionsBuffer[side.ordinal()] = false;
			pipeTDConnectionsBuffer[side.ordinal()] = false;
		}
	}

	/** Used by RegisterCapabilitiesEvent wiring in LPRegistries. */
	@Nullable
	public IItemHandler getItemHandlerForSide(@Nullable Direction side) {
		return itemInsertionHandlers != null ? itemInsertionHandlers.get(side) : null;
	}

	public void scheduleRenderUpdate() {
		refreshRenderState = true;
	}

	@OnlyIn(Dist.CLIENT)
	public IIconProvider getPipeIcons() {
		if (pipe == null) {
			return null;
		}
		return pipe.getIconProvider();
	}

	@OnlyIn(Dist.CLIENT)
	public double getViewDistance() {
		return 64 * 4;
	}

	public Block getBlock() {
		return getBlockState().getBlock(); // getBlockType() removed in 1.20.1
	}

	public boolean stillValid(Player player) {
		return level.getBlockEntity(worldPosition) == this;
	}

	@Override
	public boolean isRemoved() {
		if (pipe != null && pipe.preventRemove()) {
			return false;
		}
		return super.isRemoved();
	}

	@Override
	public LogicController getLogicController() {
		return logicController;
	}

	@Override
	public ILPPipe getLPPipe() {
		return pipe;
	}

	@Override
	public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double traveled, double max,
			List<DoubleCoordinates> visited) {
		if (pipe == null || traveled > max) {
			return Integer.MAX_VALUE;
		}
		double result = pipe.getDistanceTo(destinationint, ignore, ident, isActive, traveled + getDistance(), max, visited);
		if (result == Integer.MAX_VALUE) {
			return result;
		}
		return result + (int) getDistance();
	}

	@Override
	public boolean acceptItem(LPTravelingItem item, BlockEntity from) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport != null) {
			pipe.transport.injectItem(item, item.output);
			return true;
		}
		return false;
	}

	@Override
	public void refreshTileCacheOnSide(Direction side) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			cache[side.ordinal()].refresh();
		}
	}

	public boolean nonNull() {
		return Objects.nonNull(pipe);
	}

	@Override
	public boolean isMultiBlock() {
		return nonNull() && pipe.isMultiBlock();
	}

	public boolean isPipeBlock() {
		return nonNull() && pipe.isPipeBlock();
	}

	@Override
	public Stream<BlockEntity> getPartsOfPipe() {
		return this.subMultiBlock.stream().map(pos -> pos.getTileEntity(level));
	}

	@Nullable
	public IItemHandler getItemCap(@Nullable Direction side) {
		if (side != null) {
			return getItemHandlerForSide(side);
		}
		return null;
	}

	@Nullable
	public IFluidHandler getFluidCap(@Nullable Direction side) {
		if (side != null && pipe != null && pipe.transport instanceof PipeFluidTransportLogistics fluidTransport) {
			return fluidTransport.getIFluidHandler(side);
		}
		return null;
	}

	public static class CoreState implements IClientState {

		public String pipeIdName = "";

		@Override
		public void writeData(LPDataOutput output) {
			output.writeUTF(pipeIdName == null ? "" : pipeIdName);
		}

		@Override
		public void readData(LPDataInput input) {
			pipeIdName = input.readUTF();
		}
	}
}
